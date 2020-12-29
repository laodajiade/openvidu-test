package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.core.*;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.GeoLocation;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author geedow
 * @date 2019/11/5 17:10
 */
@Slf4j
@Service
public class JoinRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.JOINROOM_ROOM_PARAM);
        String clientMetadata = getStringParam(request, ProtocolElements.JOINROOM_METADATA_PARAM);
        OpenViduRole role = OpenViduRole.valueOf(getStringParam(request, ProtocolElements.JOINROOM_ROLE_PARAM));
        String secret = getStringParam(request, ProtocolElements.JOINROOM_SECRET_PARAM);
        String platform = getStringParam(request, ProtocolElements.JOINROOM_PLATFORM_PARAM);
        StreamType streamType = StreamType.valueOf(getStringParam(request, ProtocolElements.JOINROOM_STREAM_TYPE_PARAM));
        String password = getStringOptionalParam(request, ProtocolElements.JOINROOM_PASSWORD_PARAM);
        String moderatorPassword = getStringOptionalParam(request, ProtocolElements.JOINROOM_MODERATORPASSWORD_PARAM);
        String ruid = getStringOptionalParam(request, ProtocolElements.JOINROOM_RUID_PARAM);
        boolean isReconnected = getBooleanParam(request, ProtocolElements.JOINROOM_ISRECONNECTED_PARAM);
        String micStatus = getStringOptionalParam(request, ProtocolElements.JOINROOM_MICSTATUS_PARAM);
        String videoStatus = getStringOptionalParam(request, ProtocolElements.JOINROOM_VIDEOSTATUS_PARAM);
        String participantPrivatetId = rpcConnection.getParticipantPrivateId();
        SessionPreset preset = sessionManager.getPresetInfo(sessionId);
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;
        VoiceMode voiceMode = VoiceMode.off;
        rpcConnection.setReconnected(isReconnected);

        try {
            do {
                // verify room join type
                String joinType = getStringParam(request, ProtocolElements.JOINROOM_TYPE_PARAM);


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
                            errCode = ErrorCodeEnum.CONFERENCE_IS_FINISHED;
                            break;
                        }
                    }
                    log.error("can not find roomId:{} in data layer", sessionId);
                    errCode = ErrorCodeEnum.CONFERENCE_NOT_EXIST;
                    break;
                }

                // v.1.3.2 如果会议室有人，则不删除房间，无法通过预约邀请新加入房间
                // 未来的版本会对预约会议做功能上优化，这段代码可能可以删除
                if (!Objects.equals(joinType, ParticipantJoinType.invited.name()) && (!Objects.isNull(sessionManager.getSession(sessionId)))
                        && sessionManager.getSession(sessionId).getRuid().startsWith("appt-")) {
                    AppointConference appointConference = appointConferenceManage.getByRuid(sessionManager.getSession(sessionId).getRuid());
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

                if (!rpcConnection.isReconnected() && StreamType.MAJOR.equals(streamType) &&
                        SessionPresetUseIDEnum.ONLY_MODERATOR.equals(preset.getUseIdTypeInRoom()) ) {
                    if (!isModerator(role) && ParticipantJoinType.active.equals(ParticipantJoinType.valueOf(joinType))) {
                        log.error("disable participant active join room:{}", sessionId);
                        errCode = ErrorCodeEnum.CONFERENCE_NOT_EXIST;
                        break;
                    }
                }

                // verify conference password
                if (StreamType.MAJOR.equals(streamType) && !Objects.equals(joinType, ParticipantJoinType.invited.name())
                        && !StringUtils.isEmpty(conference.get(0).getPassword()) && !Objects.equals(conference.get(0).getPassword(), password)
                        && StringUtils.isEmpty(moderatorPassword) && OpenViduRole.MODERATOR != role) {
                    log.error("invalid room password:{}", password);
                    errCode = ErrorCodeEnum.CONFERENCE_PASSWORD_ERROR;
                    break;
                }

                // verify conference ever locked
                if (!rpcConnection.isReconnected() && !Objects.isNull(sessionManager.getSession(sessionId)) &&
                        sessionManager.getSession(sessionId).isLocking()) {
                    log.error("room:{} is locked.", sessionId);
                    errCode = ErrorCodeEnum.CONFERENCE_IS_LOCKED;
                    break;
                }

                // remove previous participant if reconnect
                if (StreamType.MAJOR.equals(streamType) && AccessTypeEnum.terminal.equals(rpcConnection.getAccessType())) {
                    Map partInfo = cacheManage.getPartInfo(rpcConnection.getUserUuid());
                    Session session = sessionManager.getSession(sessionId);
                    String roomId = Objects.isNull(!partInfo.isEmpty() ? partInfo.get("roomId") : null) ? null : partInfo.get("roomId").toString();
                    if (!partInfo.isEmpty() && Objects.nonNull(session) && sessionId.equals(roomId)) {
                        log.info("remove previous participant if reconnect " + partInfo.toString());
                        if (!CollectionUtils.isEmpty(session.getParticipants())) {
                            Participant originalPart = session.getParticipantByUUID(rpcConnection.getUserUuid());
                            if (originalPart != null) {
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

                                //save previous order if reconnect
                                sessionManager.getSession(sessionId).saveOriginalPartOrder(originalPart);
                            }else{
                                log.warn("reconnect warn,because originalPart is null"); // todo yy 重点监听下这个日志输出,按流程不应该会是null
                            }

                        }
                        //记录与会者断线前的音视频状态
                        if (Objects.nonNull(partInfo.get(ProtocolElements.JOINROOM_MICSTATUS_PARAM))) {
                            micStatus = partInfo.get(ProtocolElements.JOINROOM_MICSTATUS_PARAM).toString();
                        }
                        if (Objects.nonNull(partInfo.get(ProtocolElements.JOINROOM_VIDEOSTATUS_PARAM))) {
                            videoStatus = partInfo.get(ProtocolElements.JOINROOM_VIDEOSTATUS_PARAM).toString();
                        }

                        sessionManager.evictParticipantByUUID(roomId, rpcConnection.getUserUuid(),
                                Collections.singletonList(EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION));
                    }

                    if (!partInfo.isEmpty() && Objects.nonNull(roomId) && !sessionId.equals(roomId)) {
                        log.info("参会者加入不同的会议室，踢出上个会议室,{},{},{}", roomId, rpcConnection.getUserUuid(), sessionId);
                        sessionManager.evictParticipantByUUID(roomId, rpcConnection.getUserUuid(),
                                Collections.singletonList(EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION));
                    }
                }

                // check ever joinRoom duplicately
                /*if (!isReconnected && StreamType.MAJOR.equals(streamType) && sessionManager.joinRoomDuplicately(rpcConnection.getUserUuid())) {
//                if (!isReconnected  && sessionManager.joinRoomDuplicately(sessionId, rpcConnection.getUserUuid(), streamType)) {
                    errCode = ErrorCodeEnum.JOIN_ROOM_DUPLICATELY;
                    break;
                }*/

                GeoLocation location = null;
                boolean recorder = false;

                // verify room capacity limit.
                if (!Objects.isNull(sessionManager.getSession(sessionId)) && !Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web)) {
                    Set<Participant> majorParts = sessionManager.getSession(sessionId).getMajorPartEachConnect();
                    if (StreamType.MAJOR.equals(streamType) && majorParts.size() > preset.getRoomCapacity() - 1) {
                        log.error("verify room:{} capacity:{} cur capacity:{}", sessionId, preset.getRoomCapacity(), majorParts.size());
                        errCode = ErrorCodeEnum.ROOM_CAPACITY_PERSONAL_LIMITED;
                        break;
                    }
                }
                //判断发起会议时是否超出企业人数上限
                if (StreamType.MAJOR.equals(streamType) && !Objects.isNull(sessionManager.getSession(sessionId))) {
                    String project  = sessionManager.getSession(sessionId).getConference().getProject();
                    Collection<Session> sessions = sessionManager.getSessions();
                    if (Objects.nonNull(sessions)) {
                        AtomicInteger limitCapacity = new AtomicInteger();
                        sessions.forEach(e -> {
                            if (project.equals(e.getConference().getProject())) {
                                limitCapacity.addAndGet(e.getMajorPartEachConnect().size());
                            }
                        });
                        //query sd_corporation info
                        Corporation corporation = corporationMapper.selectByCorpProject(project);
                        if (Objects.nonNull(corporation.getCapacity()) && limitCapacity.get() > corporation.getCapacity() - 1) {
                            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                                    null, ErrorCodeEnum.ROOM_CAPACITY_CORP_LIMITED);
                            return;
                        }
                    }
                }
                //判断通话时长是否不足
                if (!Objects.isNull(sessionManager.getSession(sessionId))) {
                    String project  = sessionManager.getSession(sessionId).getConference().getProject();
                    Corporation corporation = corporationMapper.selectByCorpProject(project);
                    if (Objects.nonNull(corporation) && corporation.getRemainderDuration() <= 0) {
                        notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                                null, ErrorCodeEnum.REMAINDER_DURATION_USE_UP);
                        return;
                    }
                }


                try {
                    recorder = getBooleanParam(request, ProtocolElements.JOINROOM_RECORDER_PARAM);
                } catch (RuntimeException e) {
                    // Nothing happens. 'recorder' param to false
                }

                boolean generateRecorderParticipant = false;
                if (openviduConfig.isOpenViduSecret(secret)) {
                    sessionManager.newInsecureParticipant(participantPrivatetId);
                    if (recorder) {
                        generateRecorderParticipant = true;
                    }
                }

                if (!sessionManager.formatChecker.isServerMetadataFormatCorrect(clientMetadata)) {
                    log.error("Metadata format set in client-side is incorrect");
                    errCode = ErrorCodeEnum.SERVER_UNKNOWN_ERROR;
                    break;
                }

                JsonObject clientMetadataObj = gson.fromJson(clientMetadata, JsonObject.class);
                clientMetadataObj.addProperty("account", rpcConnection.getUserUuid());
                // change participant role if web THOR invite the same user
                if (!Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web) && !Objects.isNull(sessionManager.getSession(sessionId))) {
                    Participant thorPart = sessionManager.getSession(sessionId).getParticipants().stream().filter(part -> Objects.equals(OpenViduRole.THOR,
                            part.getRole())).findFirst().orElse(null);
                    if (!Objects.isNull(thorPart) && thorPart.getUuid().equals(clientMetadataObj.get("account").getAsString()) &&
                            !Objects.equals(OpenViduRole.THOR, role) && streamType.equals(StreamType.MAJOR)) {
                        role = OpenViduRole.MODERATOR;
                        clientMetadataObj.addProperty("role", OpenViduRole.MODERATOR.name());
                        log.info("change participant role cause web THOR invite the same userId:{}", rpcConnection.getUserId());
                    }
                }

                clientMetadataObj.addProperty("role", role.name());

                clientMetadata = clientMetadataObj.toString();

                // check ever already exits share part
                Session session;
                if (StreamType.SHARING.equals(streamType) && Objects.nonNull(session = sessionManager.getSession(sessionId))
                        && session.getParticipants().stream().anyMatch(participant -> StreamType.SHARING.equals(participant.getStreamType()))) {
                    errCode = ErrorCodeEnum.SHARING_ALREADY_EXISTS;
                    break;
                }

                Participant participant;
                if (generateRecorderParticipant) {
                    participant = sessionManager.newRecorderParticipant(rpcConnection.getUserId(), sessionId, participantPrivatetId, clientMetadata, role.name(), streamType.name());
                } else {
                    participant = sessionManager.newParticipant(rpcConnection.getUserId(), sessionId, participantPrivatetId, clientMetadata,
                            role.name(), streamType.name(), location, platform, participantPrivatetId.substring(0, Math.min(16, participantPrivatetId.length())), rpcConnection.getAbility(), rpcConnection.getFunctionality());
                }

                Long userId = rpcConnection.getUserId();
                String serialNumber = rpcConnection.getSerialNumber();
                String participantName = sessionId + "_" + rpcConnection.getUserUuid() + "_" + streamType.name();
                participant.setPreset(preset);
                participant.setJoinType(ParticipantJoinType.valueOf(joinType));
                participant.setParticipantName(participantName);
                participant.setAbility(rpcConnection.getAbility());
                participant.setFunctionality(rpcConnection.getFunctionality());
                participant.setUserType(rpcConnection.getUserType());
                participant.setTerminalType(rpcConnection.getTerminalType());
                participant.setUuid(rpcConnection.getUserUuid());
                participant.setUsername(rpcConnection.getUsername());
                participant.setProject(rpcConnection.getProject());
                participant.setMicStatus(StringUtils.isEmpty(micStatus) ? ParticipantMicStatus.on : ParticipantMicStatus.valueOf(micStatus));
                participant.setVideoStatus(StringUtils.isEmpty(videoStatus) ? ParticipantVideoStatus.on : ParticipantVideoStatus.valueOf(videoStatus));
                participant.setVoiceMode(voiceMode);
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

                sessionManager.joinRoom(participant, sessionId, conference.get(0), request.getId());
            } while (false);

            rpcConnection.setReconnected(false);
            if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
                this.notificationService.sendErrorResponseWithDesc(participantPrivatetId, request.getId(),
                        null, errCode);

                log.error("join room:{} failed. errCode:{} message:{}", sessionId, errCode.getCode(), errCode.getMessage());
                if (isModerator(role)) {
                    sessionManager.cleanCacheCollections(sessionId);
                    cleanSession(sessionId, rpcConnection.getParticipantPrivateId(), false, EndReason.forceCloseSessionByUser);
                }
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
            log.error("joinRoom error ", e);
            if (isModerator(role)) {
                sessionManager.cleanCacheCollections(sessionId);
            }
        }
    }

}
