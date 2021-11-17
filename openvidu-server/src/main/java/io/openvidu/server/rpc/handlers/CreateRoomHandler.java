package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.dao.FixedRoomManagerMapper;
import io.openvidu.server.common.dao.FixedRoomMapper;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.core.SessionPresetEnum;
import io.openvidu.server.exception.BizException;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.service.CorpInfoService;
import io.openvidu.server.service.SessionEventRecord;
import io.openvidu.server.utils.RandomRoomIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author geedow
 * @date 2019/11/5 16:18
 */
@Slf4j
@Service
public class CreateRoomHandler extends RpcAbstractHandler {

    @Autowired
    private AppointConferenceManage appointConferenceManage;

    @Autowired
    private AppointConferenceMapper appointConferenceMapper;

    @Autowired
    private RandomRoomIdGenerator randomRoomIdGenerator;

    @Autowired
    private InviteParticipantHandler inviteParticipantHandler;

    @Resource
    private ConferencePartHistoryMapper conferencePartHistoryMapper;
    @Autowired
    private FixedRoomMapper fixedRoomMapper;
    @Autowired
    private FixedRoomManagerMapper fixedRoomManagerMapper;

    @Autowired
    CorpInfoService corpInfoService;


    private static final ReentrantLock GLOBAL_LOCK = new ReentrantLock();


    @Value("${eureka.instance.instance-id}")
    private String instanceId;

    @Transactional
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ID_PARAM);
        String password = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_PASSWORD_PARAM);
        String roomSubject = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SUBJECT_PARAM);
        String ruid = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_RUID_PARAM);
        ConferenceModeEnum conferenceMode = ConferenceModeEnum.valueOf(getStringParam(request,
                ProtocolElements.CREATE_ROOM_CONFERENCE_MODE_PARAM));
        RoomIdTypeEnums roomIdType = RoomIdTypeEnums.parse(getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ID_TYPE_PARAM, "personal"));

        AppointConference appt = null;
        String moderatorUuid = rpcConnection.getUserUuid();


        if (StringUtils.isEmpty(ruid) && (RoomIdTypeEnums.random == roomIdType || StringUtils.isEmpty(sessionId))) {
            sessionId = randomRoomIdGenerator.offerRoomId();
        } else if (!StringUtils.isEmpty(ruid) && ruid.startsWith("appt-")) {
            appt = appointConferenceManage.getByRuid(ruid);
            if (appt == null) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.APPOINTMENT_CONFERENCE_NOT_EXIST);
                return;
            }
            if (appt.getStatus() == ConferenceStatus.FINISHED.getStatus()) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.APPOINTMENT_CONFERENCE_HAS_FINISHED);
                return;
            }
            sessionId = appt.getRoomId();
            roomIdType = RoomIdTypeEnums.calculationRoomType(sessionId);
            moderatorUuid = appt.getModeratorUuid();
        } else if (!StringUtils.isEmpty(ruid)) {
            Conference conference = conferenceMapper.selectByRuid(ruid);
            if (conference == null) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
                return;
            }
            if (conference.getStatus() == ConferenceStatus.FINISHED.getStatus()) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED);
                return;
            }
            sessionId = conference.getRoomId();
            roomIdType = RoomIdTypeEnums.parse(conference.getRoomIdType());
        } else {
            roomIdType = RoomIdTypeEnums.calculationRoomType(sessionId);
        }


        ErrorCodeEnum result;
        if ((result = checkService(rpcConnection, sessionId)) != ErrorCodeEnum.SUCCESS) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, result);
            return;
        }

        Optional<Conference> processConference;
        if ((processConference = getProcessConference(sessionId)).isPresent()) {
            if (roomIdType == RoomIdTypeEnums.fixed) {
                if (!Objects.equals(ruid, processConference.get().getRuid())) {
                    notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                            null, ErrorCodeEnum.ROOM_IS_IN_USE);
                    return;
                }
            } else if (!StringUtils.isEmpty(ruid) || roomIdType == RoomIdTypeEnums.personal) { // 如果是预约会议已开始或个人会议室则假装创建成功
                JsonObject respJson = new JsonObject();
                respJson.addProperty(ProtocolElements.CREATE_ROOM_ID_PARAM, sessionId);
                respJson.addProperty(ProtocolElements.CREATE_ROOM_RUID_PARAM, processConference.get().getRuid());
                respJson.addProperty("inviteUrl", processConference.get().getShortUrl() == null ?
                        "" : openviduConfig.getConferenceInviteUrl() + processConference.get().getShortUrl());
                log.info("param ruid={}, actual ruid = {}", ruid, processConference.get().getRuid());
                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
                return;
            }

            log.warn("conference:{} already exist.", sessionId);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
            return;
        }

        CountDownLatch inviteCountDownLatch = new CountDownLatch(1);
        try {
            if (!GLOBAL_LOCK.tryLock(2, TimeUnit.SECONDS)) {
                log.info("create room lock timeout {}", sessionId);
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.JOIN_ROOM_TIMEOUT);
            }
            if (sessionManager.isNewSessionIdValid(sessionId)) {
                Conference conference = new Conference();
                // if create appointment conference
                if (!StringUtils.isEmpty(ruid) && ruid.startsWith("appt-") && appt != null) {
                    Date date = new Date();
                    Calendar instance = Calendar.getInstance();
                    instance.setTime(date);
                    instance.add(Calendar.MINUTE, appt.getDuration());
                    appointConferenceMapper.changeStatusByRuid(ConferenceStatus.PROCESS.getStatus(), ruid, date, instance.getTime());

                    conference.setRuid(ruid);
                    roomSubject = appt.getConferenceSubject();
                    moderatorUuid = appt.getModeratorUuid();
                    conference.setConferenceDesc(appt.getConferenceDesc());
                    final AppointConference finalAppt = appt;
                    new Thread(() -> this.inviteParticipant(inviteCountDownLatch, finalAppt)).start();
                } else {
                    conference.setRuid(UUID.randomUUID().toString());
                }

                JsonObject respJson = new JsonObject();
                respJson.addProperty(ProtocolElements.CREATE_ROOM_ID_PARAM, sessionId);
                // save conference info
                conference.setRoomId(sessionId);
                conference.setConferenceSubject(roomSubject);
                conference.setConferenceMode(conferenceMode.getMode());
                conference.setUserId(rpcConnection.getUserId());
                conference.setPassword(StringUtils.isEmpty(password) ? null : password);
                conference.setStatus(1);
                conference.setStartTime(new Date());
                conference.setProject(rpcConnection.getProject());
                conference.setRoomIdType(roomIdType.name());
                conference.setModeratorUuid(moderatorUuid);
                conference.setShortUrl(roomManage.createShortUrl());
                conference.setModeratorName(rpcConnection.getUsername());
                conference.setOpenviduId(instanceId);
                roomManage.createMeetingRoom(conference);

                // setPresetInfo.
                String micStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_MIC_STATUS_PARAM);
                String videoStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_VIDEO_STATUS_PARAM);
                String sharePowerInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SHARE_POWER_PARAM);
                Integer roomCapacity = getIntOptionalParam(request, ProtocolElements.CREATE_ROOM_ROOM_CAPACITY_PARAM);
                Float roomDuration = getFloatOptionalParam(request, ProtocolElements.CREATE_ROOM_DURATION_PARAM);
                String useIdInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_USE_ID_PARAM);
                String allowPartDismissMute = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOWPARTDISMISSMUTE_PARAM);
                String allowPartOperShare = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOW_PART_OPER_SHARE_PARAM);
                String quietStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_QUIET_STATUS_PARAM);
                Corporation corporation = corpInfoService.selectByCorpProject(rpcConnection.getProject());
                SessionPreset preset = new SessionPreset(micStatusInRoom, videoStatusInRoom, sharePowerInRoom,
                        roomSubject, roomCapacity, roomDuration, useIdInRoom, allowPartDismissMute, allowPartOperShare,
                        quietStatusInRoom, instanceId);
                preset.setMcuThreshold(corporation.getMcuThreshold());
                preset.setSfuPublisherThreshold(corporation.getSfuPublisherThreshold());
                if (roomIdType == RoomIdTypeEnums.fixed) {
                    FixedRoom fixedRoom = fixedRoomMapper.selectByRoomId(sessionId);
                    preset.setAllowRecord(fixedRoom.getAllowRecord() ? SessionPresetEnum.on : SessionPresetEnum.off);
                    preset.setAllowPart(fixedRoom.getAllowPart());
                    preset.setRoomCapacity(fixedRoom.getRoomCapacity());
                    preset.setShortId(fixedRoom.getShortId());
                }

                Session session = sessionManager.createSession(sessionId, conference, preset);
                if (appt != null) {
                    session.setEndTime(appt.getEndTime().getTime());
                }
                inviteCountDownLatch.countDown();
                SessionEventRecord.createRoom(session);
                respJson.addProperty("inviteUrl", conference.getShortUrl() == null ?
                        "" : openviduConfig.getConferenceInviteUrl() + conference.getShortUrl());
                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
            } else {
                log.warn("conference:{} already exist.", sessionId);
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
            }
        } catch (BizException e) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, e.getRespEnum());
        } catch (Exception e) {
            log.error("create Room error room_id = {}", sessionId, e);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
        } finally {
            GLOBAL_LOCK.unlock();
        }

    }

    protected Optional<Conference> getProcessConference(String sessionId) {
        Conference conference = conferenceMapper.selectUsedConference(sessionId);
        return Optional.ofNullable(conference);
    }

    private ErrorCodeEnum checkService(RpcConnection rpcConnection, String roomId) {
        if (RoomIdTypeEnums.calculationRoomType(roomId) == RoomIdTypeEnums.fixed) {
            return fixedVerification(rpcConnection, roomId);
        } else {
            return generalVerification(rpcConnection);
        }
    }

    private ErrorCodeEnum fixedVerification(RpcConnection rpcConnection, String roomId) {
        FixedRoom fixedRoom = fixedRoomMapper.selectByRoomId(roomId);
        if (fixedRoom == null || fixedRoom.getStatus() == 0) {
            return ErrorCodeEnum.CONFERENCE_NOT_EXIST;
        }

        if (fixedRoom.getStatus() == 2 || fixedRoom.getExpireDate().isBefore(LocalDateTime.now())) {
            return ErrorCodeEnum.FIXED_ROOM_EXPIRED;
        }

        FixedRoomManager fixedRoomManager = fixedRoomManagerMapper.selectByUserId(rpcConnection.getUserId(), fixedRoom.getRoomId());
        if (fixedRoomManager == null) {
            return ErrorCodeEnum.PERMISSION_LIMITED;
        }
        return ErrorCodeEnum.SUCCESS;
    }

    private ErrorCodeEnum generalVerification(RpcConnection rpcConnection) {
        Corporation corporation = corpInfoService.selectByCorpProject(rpcConnection.getProject());

        if (!corporationMapper.isConcurrentServiceDuration(corporation)) {
            return ErrorCodeEnum.SERVICE_NOT_ACTIVATION_OR_EXPIRED;
        }

        //判断发起会议时是否超出企业人数上限
        Collection<Session> sessions = sessionManager.getSessions();

        if (Objects.nonNull(sessions)) {
            AtomicInteger limitCapacity = new AtomicInteger();
            sessions.forEach(e -> {
                if (rpcConnection.getProject().equals(e.getConference().getProject())) {
                    limitCapacity.addAndGet(e.getPartSize());
                }
            });
            //query sd_corporation info
            if (Objects.nonNull(corporation.getCapacity()) && limitCapacity.get() > corporation.getCapacity() - 1) {
                return ErrorCodeEnum.ROOM_CAPACITY_CORP_LIMITED;
            }
        }
        //判断通话时长是否不足
        if (Objects.nonNull(corporation) && corporation.getRemainderDuration() <= 0) {
            return ErrorCodeEnum.REMAINDER_DURATION_USE_UP;
        }


        return ErrorCodeEnum.SUCCESS;
    }

    public void inviteParticipant(CountDownLatch countDownLatch, AppointConference appt) {
        if (!appt.getAutoInvite().equals(AutoInviteEnum.AUTO_INVITE.getValue())) {
            return;
        }

        try {
            if (countDownLatch.await(10, TimeUnit.SECONDS)) {
                // wait 1 second
                TimeUnit.SECONDS.sleep(2);

                Map userInfo = cacheManage.getUserInfoByUUID(appt.getModeratorUuid());
                Object username = userInfo.get("username");
                String deviceName = userInfo.containsKey("deviceName") ? String.valueOf(userInfo.get("deviceName")) : null;

                JsonObject params = new JsonObject();
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_ID_PARAM, appt.getRoomId());
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM, appt.getModeratorUuid());
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERNAME_PARAM, Objects.isNull(username) ? deviceName : username.toString());
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERICON_PARAM, "");
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_EXPIRETIME_PARAM, String.valueOf(System.currentTimeMillis() + 60000));
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SUBJECT_PARAM, appt.getConferenceSubject());
                if (!StringUtils.isEmpty(deviceName)) {
                    params.addProperty(ProtocolElements.INVITE_PARTICIPANT_DEVICE_NAME_PARAM, deviceName);
                }

                List<User> users = conferencePartHistoryMapper.selectUserByRuid(appt.getRuid());

                List<String> targets = users.stream().filter(user -> !appt.getModeratorUuid().equals(user.getUuid()))
                        .map(User::getUuid).collect(Collectors.toList());
                targets.forEach(uuid -> {
                    cacheManage.saveInviteInfo(appt.getRoomId(), uuid);
                });
                log.info("invite participant in create room, targets = {}", targets);
                inviteParticipantHandler.inviteOnline(targets, params);
            } else {
                log.info("cancel invite participant in create room roomId = {}, ruid = {}", appt.getRoomId(), appt.getRuid());
            }
        } catch (InterruptedException e) {
            log.error("create room inviteParticipant error", e);
        }
    }
}
