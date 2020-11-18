package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.RandomRoomIdGenerator;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

    @Transactional
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ID_PARAM);
        String password = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_PASSWORD_PARAM);
        String roomSubject = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SUBJECT_PARAM);
        String ruid = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_RUID_PARAM);
        String moderatorPassword = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_MODERATORPASSWORD_PARAM);
        ConferenceModeEnum conferenceMode = ConferenceModeEnum.valueOf(getStringParam(request,
                ProtocolElements.CREATE_ROOM_CONFERENCE_MODE_PARAM));
        String roomIdType = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ID_TYPE_PARAM, "personal");

        if (StringUtils.isEmpty(ruid) && (RoomIdTypeEnums.random.name().equals(roomIdType) || StringUtils.isEmpty(sessionId))) {
            sessionId = randomRoomIdGenerator.offerRoomId();
        }
        //判断发起会议时是否超出企业人数上限
        Collection<Session> sessions = sessionManager.getSessions();
        if (Objects.nonNull(sessions)) {
            AtomicInteger limitCapacity = new AtomicInteger();
            sessions.forEach(e -> {
                if (rpcConnection.getProject().equals(e.getConference().getProject())) {
                    limitCapacity.addAndGet(e.getMajorPartEachConnect().size());
                }
            });
            //query sd_corporation info
            Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
            if (Objects.nonNull(corporation.getCapacity()) && limitCapacity.get() > corporation.getCapacity() - 1) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.ROOM_CAPACITY_CORP_LIMITED);
                return;
            }
        }
        //判断通话时长是否不足
        Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
        if (Objects.nonNull(corporation) && corporation.getRemainderDuration() <= 0) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REMAINDER_DURATION_USE_UP);
            return;
        }

        AppointConference appt = null;
        String moderatorUuid = rpcConnection.getUserUuid();
        if (!StringUtils.isEmpty(ruid)) {
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
            roomIdType = "random";
            moderatorUuid = appt.getModeratorUuid();
        }

        if (isExistingRoom(sessionId, rpcConnection.getUserUuid())) {
            // 如果是预约会议已开始则假装创建成功
            if (!StringUtils.isEmpty(ruid)) {
                JsonObject respJson = new JsonObject();
                respJson.addProperty(ProtocolElements.CREATE_ROOM_ID_PARAM, sessionId);
                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
                return;
            }

            log.warn("conference:{} already exist.", sessionId);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        if (sessionManager.isNewSessionIdValid(sessionId)) {
            Conference conference = new Conference();
            // if create appointment conference
            if (!StringUtils.isEmpty(ruid)) {
                appointConferenceMapper.changeStatusByRuid(ConferenceStatus.PROCESS.getStatus(), ruid);

                conference.setRuid(ruid);
                roomSubject = appt.getConferenceSubject();
                conference.setConferenceDesc(appt.getConferenceDesc());
                final AppointConference finalAppt = appt;
                new Thread(() -> this.inviteParticipant(countDownLatch, finalAppt)).start();
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
            conference.setModeratorPassword(StringUtils.isEmpty(moderatorPassword) ? StringUtil.getRandomPassWord(6) : moderatorPassword);
            conference.setRoomIdType(roomIdType);
            conference.setModeratorUuid(moderatorUuid);
            roomManage.createMeetingRoom(conference);

            // setPresetInfo.
            String micStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_MIC_STATUS_PARAM);
            String videoStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_VIDEO_STATUS_PARAM);
            String sharePowerInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SHARE_POWER_PARAM);
            Integer roomCapacity = getIntOptionalParam(request, ProtocolElements.CREATE_ROOM_ROOM_CAPACITY_PARAM);
            Float roomDuration = getFloatOptionalParam(request, ProtocolElements.CREATE_ROOM_DURATION_PARAM);
            String useIdInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_USE_ID_PARAM);
            String allowPartOperMic = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOW_PART_OPER_MIC_PARAM);
            String allowPartOperShare = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOW_PART_OPER_SHARE_PARAM);
            String quietStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_QUIET_STATUS_PARAM);

            SessionPreset preset = new SessionPreset(micStatusInRoom, videoStatusInRoom, sharePowerInRoom,
                    roomSubject, roomCapacity, roomDuration, useIdInRoom, allowPartOperMic, allowPartOperShare, quietStatusInRoom);
            sessionManager.setPresetInfo(sessionId, preset);

            // store this inactive session
            Session session = sessionManager.storeSessionNotActiveWhileRoomCreated(sessionId);
            if (appt != null) {
                session.setEndTime(appt.getEndTime().getTime());
            }
            countDownLatch.countDown();
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
        } else {
            log.warn("conference:{} already exist.", sessionId);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
        }
    }

    public void inviteParticipant(CountDownLatch countDownLatch, AppointConference appt) {
        if (!appt.getAutoInvite().equals(AutoInviteEnum.AUTO_INVITE.getValue())) {
            return;
        }

        try {
            if (countDownLatch.await(10, TimeUnit.SECONDS)) {
                // wait 1 second
                TimeUnit.SECONDS.sleep(1);

                Map userInfo = cacheManage.getUserInfoByUUID(appt.getModeratorUuid());
                Object username = userInfo.get("username");
                String deviceName = userInfo.containsKey("deviceName") ? String.valueOf(userInfo.get("deviceName")) : null;

                JsonObject params = new JsonObject();
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_ID_PARAM, appt.getRoomId());
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM, appt.getModeratorUuid());
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERNAME_PARAM, Objects.isNull(username) ? deviceName : username.toString());
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERICON_PARAM, "");
                params.addProperty(ProtocolElements.INVITE_PARTICIPANT_EXPIRETIME_PARAM, System.currentTimeMillis() + 60000);
                if (!StringUtils.isEmpty(deviceName)) {
                    params.addProperty(ProtocolElements.INVITE_PARTICIPANT_DEVICE_NAME_PARAM, deviceName);
                }

                List<User> users = conferencePartHistoryMapper.selectUserByRuid(appt.getRuid());

                List<String> targets = users.stream().map(User::getUuid).collect(Collectors.toList());
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
