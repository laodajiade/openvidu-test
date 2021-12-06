package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.client.RtcUserClient;
import io.openvidu.server.common.dao.FixedRoomMapper;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.common.pojo.vo.CallHistoryVo;
import io.openvidu.server.core.*;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.service.SessionEventRecord;
import io.openvidu.server.utils.GeoLocation;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.openvidu.server.common.enums.EvictParticipantStrategy.LAST_PARTICIPANT_LEFT;
import static java.util.Collections.singletonList;

/**
 * @author geedow
 * @date 2019/11/5 17:10
 */
@Slf4j
@Service
public class JoinRoomHandler extends RpcAbstractHandler {

    private static final RateLimiter rateLimiter = RateLimiter.create(30);

    @Autowired
    private FixedRoomMapper fixedRoomMapper;

    @Autowired
    private RtcUserClient rtcUserClient;

    @Resource
    private ApplicationContext applicationContext;

    @Value("${joinroom.rate.limiter:30}")
    public void setRateLimiter(double rate) {
        if (rate <= 0.01) {
            return;
        }
        rateLimiter.setRate(rate);
    }

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        //rate limited
        if (!rateLimiter.tryAcquire()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.RATE_LIMITER);
            return;
        }

        String sessionId = getStringParam(request, ProtocolElements.JOINROOM_ROOM_PARAM);
        String clientMetadata = getStringParam(request, ProtocolElements.JOINROOM_METADATA_PARAM);
        OpenViduRole role = OpenViduRole.valueOf(getStringParam(request, ProtocolElements.JOINROOM_ROLE_PARAM));


        String platform = getStringParam(request, ProtocolElements.JOINROOM_PLATFORM_PARAM);
        String password = getStringOptionalParam(request, ProtocolElements.JOINROOM_PASSWORD_PARAM);
        String moderatorPassword = getStringOptionalParam(request, ProtocolElements.JOINROOM_MODERATORPASSWORD_PARAM);
        String ruid = getStringOptionalParam(request, ProtocolElements.JOINROOM_RUID_PARAM);
        boolean isReconnected = getBooleanParam(request, ProtocolElements.JOINROOM_ISRECONNECTED_PARAM);
        String micStatus = getStringOptionalParam(request, ProtocolElements.JOINROOM_MICSTATUS_PARAM);
        String videoStatus = getStringOptionalParam(request, ProtocolElements.JOINROOM_VIDEOSTATUS_PARAM);
        String nickName = getStringOptionalParam(request, ProtocolElements.JOINROOM_NICKNAME_PARAM);
        String participantPrivatetId = rpcConnection.getParticipantPrivateId();

        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;
        VoiceMode voiceMode = VoiceMode.off;
        rpcConnection.setReconnected(isReconnected);

        String speakerStatus = "on";
        Session session = null;
        try {
            do {
                //保存游客nickName
                if (!StringUtils.isEmpty(nickName)) {
                    rpcConnection.setUsername(nickName);
                }

                // verify room join type
                String joinType = getStringParam(request, ProtocolElements.JOINROOM_TYPE_PARAM);

                // 短号入会
                if (RoomIdTypeEnums.isShortId(sessionId)) {
                    FixedRoom fixedRoom = fixedRoomMapper.selectByShortId(sessionId, rpcConnection.getCorpId());
                    if (fixedRoom == null) {
                        errCode = ErrorCodeEnum.CONFERENCE_NOT_EXIST;
                        break;
                    }
                    sessionId = fixedRoom.getRoomId();
                }

                ConferenceSearch search = new ConferenceSearch();
                search.setRoomId(sessionId);
                search.setStatus(1);
                List<Conference> conference = conferenceMapper.selectBySearchCondition(search);
                if (conference == null || conference.isEmpty()) {
                    if (StringUtils.startsWithIgnoreCase(ruid, "appt-")) {
                        AppointConference ac = appointConferenceManage.getByRuid(ruid);
                        if (ac != null && ac.getStartTime().after(new Date())) {
                            log.error("can not find roomId:{} and appointment conference {} did not start", sessionId, ruid);
                            errCode = ErrorCodeEnum.APPOINTMENT_CONFERENCE_DID_NOT_START;
                            break;
                        }
                    } else if (!StringUtils.isEmpty(ruid)) {
                        Conference conference0 = conferenceMapper.selectByRuid(ruid);
                        if (conference0 != null && conference0.getStatus() == ConferenceStatus.FINISHED.getStatus()) {
                            errCode = ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED;
                            break;
                        }
                    }
                    log.error("can not find roomId:{} in data layer", sessionId);
                    errCode = ErrorCodeEnum.CONFERENCE_NOT_EXIST;
                    break;
                } else {
                    Conference cfc = conference.get(0);
                    if (cfc.getRoomIdType().equals("fixed") && StringUtils.startsWithIgnoreCase(ruid, "appt-")
                            && !Objects.equals(ruid, cfc.getRuid())) {
                        AppointConference appointConference = appointConferenceManage.getByRuid(ruid);
                        if (appointConference != null && appointConference.getStatus() == 0 && appointConference.getStartTime().after(new Date())) {
                            errCode = ErrorCodeEnum.APPOINTMENT_CONFERENCE_DID_NOT_START;
                        } else {
                            errCode = ErrorCodeEnum.PRE_CONFERENCE_NOT_FINISHED;
                        }
                    }
                    ruid = cfc.getRuid();
                }

                session = sessionManager.getSession(sessionId);
                if (!Objects.equals(joinType, ParticipantJoinType.invited.name()) && (!Objects.isNull(session))
                        && session.getRuid().startsWith("appt-")) {
                    AppointConference appointConference = appointConferenceManage.getByRuid(session.getRuid());
                    if (appointConference == null) {
                        errCode = ErrorCodeEnum.APPOINTMENT_CONFERENCE_NOT_EXIST;
                        break;
                    }
                }

                // if participant is moderator, change his role
                if (!Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web)
                        && Objects.equals(conference.get(0).getModeratorUuid(), rpcConnection.getUserUuid())
                ) {
                    role = OpenViduRole.MODERATOR;
                }
                SessionPreset preset = sessionManager.getPresetInfo(sessionId);
                if (!rpcConnection.isReconnected() &&
                        SessionPresetUseIDEnum.ONLY_MODERATOR.equals(preset.getUseIdTypeInRoom())) {
                    if (!isModerator(role) && ParticipantJoinType.active.equals(ParticipantJoinType.valueOf(joinType))) {
                        log.error("disable participant active join room:{}", sessionId);
                        errCode = ErrorCodeEnum.CONFERENCE_NOT_EXIST;
                        break;
                    }
                }

                //record joinRoom account
                JSONObject jsonObject = JSONObject.parseObject(clientMetadata);
                if (!OpenViduRole.MODERATOR_ROLES.contains(role)) {
                    CallHistoryVo callHistoryVo = callHistoryMapper.getCallHistoryByCondition(ruid, jsonObject.getString("account"));
                    if (Objects.isNull(callHistoryVo) && StringUtils.isEmpty(nickName)) {
                        List<CallHistory> addList = new ArrayList<>();
                        CallHistory callHistory = new CallHistory();
                        callHistory.setRoomId(sessionId);
                        callHistory.setUuid(jsonObject.getString("account"));
                        callHistory.setUsername(rpcConnection.getUsername());
                        callHistory.setRuid(ruid);
                        addList.add(callHistory);
                        if (!CollectionUtils.isEmpty(addList)) {
                            callHistoryMapper.insertBatch(addList);
                        }
                    }
                }

                // verify conference password
                if (!Objects.equals(joinType, ParticipantJoinType.invited.name())
                        && !StringUtils.isEmpty(conference.get(0).getPassword()) && !Objects.equals(conference.get(0).getPassword(), password)
                        && StringUtils.isEmpty(moderatorPassword) && OpenViduRole.MODERATOR != role) {
                    log.error("invalid room password:{}", password);
                    errCode = ErrorCodeEnum.CONFERENCE_PASSWORD_ERROR;
                    break;
                }

                // verify conference ever locked
                if (!rpcConnection.isReconnected() && !Objects.isNull(session) &&
                        session.isLocking()) {
                    log.error("room:{} is locked.", sessionId);
                    errCode = ErrorCodeEnum.CONFERENCE_IS_LOCKED;
                    break;
                }

                // remove previous participant if reconnect
                int preOrder = 0;
                if (AccessTypeEnum.terminal.equals(rpcConnection.getAccessType())) {
                    Map partInfo = cacheManage.getPartInfo(rpcConnection.getUserUuid());
                    String roomId = Objects.isNull(!partInfo.isEmpty() ? partInfo.get("roomId") : null) ? null : partInfo.get("roomId").toString();
                    if (!partInfo.isEmpty() && Objects.nonNull(session) && sessionId.equals(roomId)) {
                        log.info("remove previous participant if reconnect " + partInfo.toString());
                        if (!CollectionUtils.isEmpty(session.getParticipants())) {
                            Optional<Participant> originalPartOptional = session.getParticipantByUUID(rpcConnection.getUserUuid());
                            if (originalPartOptional.isPresent()) {
                                Participant originalPart = originalPartOptional.get();
                                //记录断线时的语音模式状态
                                voiceMode = originalPart.getVoiceMode();
                                //participant reconnect stop polling
                                if (Objects.nonNull(originalPart) && originalPart.getRole().isController()) {
                                    if (session.getPresetInfo().getPollingStatusInRoom().equals(SessionPresetEnum.on)) {
                                        SessionPreset sessionPreset = session.getPresetInfo();
                                        sessionPreset.setPollingStatusInRoom(SessionPresetEnum.off);
                                        timerManager.stopPollingCompensation(sessionId);
                                        //send stop polling notify
                                        JsonObject notifyParam = new JsonObject();
                                        notifyParam.addProperty(ProtocolElements.STOP_POLLING_ROOMID_PARAM, sessionId);
                                        notificationService.sendNotification(rpcConnection.getParticipantPrivateId(),
                                                ProtocolElements.STOP_POLLING_NODIFY_METHOD, notifyParam);
                                    }
                                }
                            } else {
                                log.warn("reconnect warn,because originalPart is null");
                            }

                        }
                        //记录与会者断线前的音视频状态
                        if (Objects.nonNull(partInfo.get(ProtocolElements.JOINROOM_MICSTATUS_PARAM))) {
                            micStatus = partInfo.get(ProtocolElements.JOINROOM_MICSTATUS_PARAM).toString();
                        }
                        if (Objects.nonNull(partInfo.get(ProtocolElements.JOINROOM_VIDEOSTATUS_PARAM))) {
                            videoStatus = partInfo.get(ProtocolElements.JOINROOM_VIDEOSTATUS_PARAM).toString();
                        }
                        if (Objects.nonNull(partInfo.get(ProtocolElements.JOINROOM_PEERSPEAKERSTATUS_PARAM))) {
                            speakerStatus = partInfo.get(ProtocolElements.JOINROOM_PEERSPEAKERSTATUS_PARAM).toString();
                        }
                        if (Objects.nonNull(partInfo.get("order"))) {
                            preOrder = Integer.parseInt(partInfo.get("order").toString());
                        }

                        sessionManager.evictParticipantByUUID(roomId, rpcConnection.getUserUuid(), Collections.emptyList(), EndReason.reconnect);
                    }

                    if (!partInfo.isEmpty() && Objects.nonNull(roomId) && !sessionId.equals(roomId)) {
                        log.info("参会者加入不同的会议室，踢出上个会议室,{},{},{}", roomId, rpcConnection.getUserUuid(), sessionId);
                        sessionManager.evictParticipantByUUIDEx(roomId, rpcConnection.getUserUuid(), Collections.emptyList(), EndReason.participantReplaceLeft);
                    }
                }

                // verify room capacity limit.
                if (!Objects.isNull(session) && !Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web)) {
                    Set<Participant> majorParts = session.getParticipants();
                    if (majorParts.size() > preset.getRoomCapacity() - 1) {
                        log.error("verify room:{} capacity:{} cur capacity:{}", sessionId, preset.getRoomCapacity(), majorParts.size());
                        errCode = ErrorCodeEnum.ROOM_CAPACITY_PERSONAL_LIMITED;
                        break;
                    }
                }
                //判断发起会议时是否超出企业人数上限
                if (!Objects.isNull(session)) {
                    String project = session.getConference().getProject();

                    //query sd_corporation info
                    int joinNum = conferencePartHistoryMapper.countProcessPartHistory(project);
                    Corporation corporation = corporationMapper.selectByCorpProject(project);
                    if (Objects.nonNull(corporation.getCapacity()) && joinNum > corporation.getCapacity() - 1) {
                        notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                                null, ErrorCodeEnum.ROOM_CAPACITY_CORP_LIMITED);
                        return;
                    }
                }
                //判断通话时长是否不足
                if (!Objects.isNull(session) && !RoomIdTypeEnums.isFixed(session.getSessionId())) {
                    String project = session.getConference().getProject();
                    Corporation corporation = corporationMapper.selectByCorpProject(project);
                    if (Objects.nonNull(corporation) && corporation.getRemainderDuration() <= 0) {
                        notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                                null, ErrorCodeEnum.REMAINDER_DURATION_USE_UP);
                        return;
                    }
                }

                boolean generateRecorderParticipant = false;

                if (!sessionManager.formatChecker.isServerMetadataFormatCorrect(clientMetadata)) {
                    log.error("Metadata format set in client-side is incorrect");
                    errCode = ErrorCodeEnum.SERVER_UNKNOWN_ERROR;
                    break;
                }

                JsonObject clientMetadataObj = gson.fromJson(clientMetadata, JsonObject.class);
                clientMetadataObj.addProperty("account", rpcConnection.getUserUuid());
                // change participant role if web THOR invite the same user
                if (!Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web) && !Objects.isNull(session)) {
                    Participant thorPart = session.getParticipants().stream().filter(part -> Objects.equals(OpenViduRole.THOR,
                            part.getRole())).findFirst().orElse(null);
                    if (!Objects.isNull(thorPart) && thorPart.getUuid().equals(clientMetadataObj.get("account").getAsString()) &&
                            !Objects.equals(OpenViduRole.THOR, role)) {
                        role = OpenViduRole.MODERATOR;
                        clientMetadataObj.addProperty("role", OpenViduRole.MODERATOR.name());
                        log.info("change participant role cause web THOR invite the same userId:{}", rpcConnection.getUserId());
                    }
                }

                clientMetadataObj.addProperty("role", role.name());

                clientMetadata = clientMetadataObj.toString();

                if (Objects.nonNull(session) && session.getPresetInfo().getAllowPart() != 0 && !Objects.equals(session.getConference().getProject(), rpcConnection.getProject())) {
                    log.info("not allow join room by outsiders,{}", rpcConnection.getUserUuid());
                    errCode = ErrorCodeEnum.CONFERENCE_NOT_EXIST;
                    break;
                }

                Participant participant;
                if (generateRecorderParticipant) {
                    participant = sessionManager.newRecorderParticipant(rpcConnection.getUserId(), sessionId, participantPrivatetId, clientMetadata, role.name());
                } else {
                    GeoLocation location = null;
                    participant = sessionManager.newParticipant(rpcConnection.getUserId(), sessionId, participantPrivatetId, clientMetadata,
                            role.name(), location, platform, rpcConnection.getDeviceModel(), rpcConnection.getAbility(), rpcConnection.getFunctionality());
                }
                participant.setApplicationContext(applicationContext);
                Long userId = rpcConnection.getUserId();
                String serialNumber = rpcConnection.getSerialNumber();
                participant.setPreset(preset);
                participant.setJoinType(ParticipantJoinType.valueOf(joinType));
                participant.setParticipantName(rpcConnection.getUsername());
                participant.setAbility(rpcConnection.getAbility());
                participant.setFunctionality(rpcConnection.getFunctionality());
                participant.setUserType(rpcConnection.getUserType());
                participant.setTerminalType(rpcConnection.getTerminalType());
                participant.setUuid(rpcConnection.getUserUuid());
                participant.setUsername(rpcConnection.getUsername());
                participant.setProject(rpcConnection.getProject());
                participant.setMicStatus(StringUtils.isEmpty(micStatus) ? ParticipantMicStatus.on : ParticipantMicStatus.valueOf(micStatus));
                participant.setVideoStatus(StringUtils.isEmpty(videoStatus) ? ParticipantVideoStatus.on : ParticipantVideoStatus.valueOf(videoStatus));
                participant.setSpeakerStatus(StringUtils.isEmpty(speakerStatus) ? ParticipantSpeakerStatus.on : ParticipantSpeakerStatus.valueOf(speakerStatus));
                participant.changeVoiceMode(voiceMode);
                participant.setOrder(preOrder);
                if (StringUtils.isEmpty(serialNumber)) {
                    if (UserType.register.equals(participant.getUserType()) && TerminalTypeEnum.S != rpcConnection.getTerminalType()) {
                        User user = userMapper.selectByPrimaryKey(userId);
                        // User and dept info.
                        UserDeptSearch udSearch = new UserDeptSearch();
                        udSearch.setUserId(userId);
                        UserDept userDeptCom = userDeptMapper.selectBySearchCondition(udSearch);
                        Department userDep = depMapper.selectByPrimaryKey(userDeptCom.getDeptId());

                        participant.setAppShowInfo(user.getUsername(), "(" + user.getTitle() + ") " + userDep.getDeptName());
                    } else {
                        participant.setAppShowInfo(rpcConnection.getUsername(), rpcConnection.getUsername());
                    }
                } else {
                    DeviceSearch devSearch = new DeviceSearch();
                    devSearch.setSerialNumber(serialNumber);
                    Device device = deviceMapper.selectBySearchCondition(devSearch);
                    DeviceDeptSearch ddSearch = new DeviceDeptSearch();
                    ddSearch.setSerialNumber(serialNumber);
                    List<DeviceDept> devDeptCom = deviceDeptMapper.selectBySearchCondition(ddSearch);
                    if (Objects.isNull(devDeptCom) || devDeptCom.isEmpty()) {
                        log.warn("devDep cant select serialNumber:{}", serialNumber);
                        participant.setAppShowInfo(device.getDeviceName(), "(" + device.getDeviceModel() + ") ");
                    } else {
                        Department devDep = depMapper.selectByPrimaryKey(devDeptCom.get(0).getDeptId());
                        participant.setAppShowInfo(device.getDeviceName(), "(" + device.getDeviceModel() + ") " + devDep.getDeptName());
                    }
                }

                rpcConnection.setSessionId(sessionId);
                rtcUserClient.updateRpcConnection(rpcConnection); //强制同步一次数据
                UseTime.point("join room p1");
                try {
                    if (session == null || session.getJoinOrLeaveReentrantLock().tryLock(1L, TimeUnit.SECONDS)) {
                        UseTime.point("join room p1.1");
                        sessionManager.joinRoom(participant, sessionId, conference.get(0), request.getId());
                        SessionEventRecord.joinRoom(session, participant, rpcConnection.isReconnected());
                    } else {
                        log.warn("{} join room timeout", rpcConnection.getUserUuid());
                        errCode = ErrorCodeEnum.RATE_LIMITER;
                    }
                } finally {
                    if (session != null) {
                        session.getJoinOrLeaveReentrantLock().unlock();
                    }
                }
            } while (false);

            rpcConnection.setReconnected(false);
            if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
                this.notificationService.sendErrorResponseWithDesc(participantPrivatetId, request.getId(),
                        null, errCode);

                log.error("join room:{} failed. errCode:{} message:{}", sessionId, errCode.getCode(), errCode.getMessage());
                /*if (isModerator(role)) {
                    sessionManager.cleanCacheCollections(sessionId);
                    cleanSession(sessionId, rpcConnection.getParticipantPrivateId(), false, EndReason.forceCloseSessionByUser);
                }*/
            } else {
                if (!Objects.isNull(rpcConnection.getSerialNumber())) {
                    cacheManage.setDeviceStatus(rpcConnection.getSerialNumber(), DeviceStatus.meeting.name());
                }

                // 预约会议实际时长从有人开始计算
                if (StringUtils.startsWithIgnoreCase(ruid, "appt-")) {
                    conferenceMapper.changeRealStartTime(ruid);
                }

            }
        } catch (Exception e) {
            log.error("joinRoom error {}, {}", request.getParams(), rpcConnection.toString(), e);
            if (isModerator(role)) {
                sessionManager.cleanCacheCollections(sessionId);
            }
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
        }
    }

}
