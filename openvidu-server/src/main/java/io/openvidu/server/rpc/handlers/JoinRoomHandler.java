package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.core.SessionPresetUseIDEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.GeoLocation;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        String role = getStringParam(request, ProtocolElements.JOINROOM_ROLE_PARAM);
        String secret = getStringParam(request, ProtocolElements.JOINROOM_SECRET_PARAM);
        String platform = getStringParam(request, ProtocolElements.JOINROOM_PLATFORM_PARAM);
        String streamType = getStringParam(request, ProtocolElements.JOINROOM_STREAM_TYPE_PARAM);
        String password = (request.getParams() != null && request.getParams().has(ProtocolElements.JOINROOM_PASSWORD_PARAM)) ?
                request.getParams().get(ProtocolElements.JOINROOM_PASSWORD_PARAM).getAsString() : null;
        String participantPrivatetId = rpcConnection.getParticipantPrivateId();
        SessionPreset preset = sessionManager.getPresetInfo(sessionId);
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;

        try {
            do {
                // verify room join type
                String joinType = getStringParam(request, ProtocolElements.JOINROOM_TYPE_PARAM);
                if (!rpcConnection.isReconnected() && StreamType.MAJOR.equals(StreamType.valueOf(streamType)) &&
                        SessionPresetUseIDEnum.ONLY_MODERATOR.equals(preset.getUseIdTypeInRoom())) {
                    if (!isModerator(role) && ParticipantJoinType.active.equals(ParticipantJoinType.valueOf(joinType))) {
                        log.error("disable participant active join room:{}", sessionId);
                        errCode = ErrorCodeEnum.PERMISSION_LIMITED;
                        break;
                    }
                }

                ConferenceSearch search = new ConferenceSearch();
                search.setRoomId(sessionId);
                search.setStatus(1);
                List<Conference> conference = conferenceMapper.selectBySearchCondition(search);
                if (conference == null || conference.isEmpty()) {
                    log.error("can not find roomId:{} in data layer", sessionId);
                    errCode = ErrorCodeEnum.CONFERENCE_NOT_EXIST;
                    break;
                }

                // verify conference password
                if (streamType.equals(StreamType.MAJOR.name()) && !Objects.equals(joinType, ParticipantJoinType.invited.name())
                        && !StringUtils.isEmpty(conference.get(0).getPassword()) && !Objects.equals(conference.get(0).getPassword(), password)) {
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
                updateReconnectInfo(rpcConnection);
                GeoLocation location = null;
                boolean recorder = false;

                // verify room capacity limit.
                if (!Objects.isNull(sessionManager.getSession(sessionId)) && !Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web)) {
                    Set<Participant> majorParts = sessionManager.getSession(sessionId).getMajorPartEachConnect();
                    if (Objects.equals(StreamType.MAJOR.name(), streamType) && majorParts.size() >= preset.getRoomCapacity()) {
                        log.error("verify room:{} capacity:{} cur capacity:{}", sessionId, preset.getRoomCapacity(), majorParts.size());
                        errCode = ErrorCodeEnum.ROOM_CAPACITY_LIMITED;
                        break;
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

                // change participant role if web THOR invite the same user
                if (!Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web) && !Objects.isNull(sessionManager.getSession(sessionId))) {
                    JsonObject clientMetadataObj = gson.fromJson(clientMetadata, JsonObject.class);
                    Participant thorPart = sessionManager.getSession(sessionId).getParticipants().stream().filter(part -> Objects.equals(OpenViduRole.THOR,
                            part.getRole())).findFirst().orElse(null);
                    if (!Objects.isNull(thorPart) && thorPart.getUserId().equals(clientMetadataObj.get("clientData").getAsString()) &&
                            !Objects.equals(OpenViduRole.THOR.name(), role) && !Objects.equals(StreamType.SHARING.name(), streamType)) {
                        role = OpenViduRole.MODERATOR.name();
                        clientMetadataObj.addProperty("role", OpenViduRole.MODERATOR.name());
                        clientMetadata = clientMetadataObj.toString();
                        log.info("change participant role cause web THOR invite the same userId:{}", rpcConnection.getUserId());
                    }
                }

                Participant participant;
                if (generateRecorderParticipant) {
                    participant = sessionManager.newRecorderParticipant(sessionId, participantPrivatetId, clientMetadata, role, streamType);
                } else {
                    participant = sessionManager.newParticipant(sessionId, participantPrivatetId, clientMetadata,
                            role, streamType, location, platform,
                            participantPrivatetId.substring(0, Math.min(16, participantPrivatetId.length())));
                }

                Long userId = rpcConnection.getUserId();
                String serialNumber = rpcConnection.getSerialNumber();
                String participantName = sessionId + "_" + rpcConnection.getUserUuid() + "_" + streamType;
                participant.setPreset(preset);
                participant.setJoinType(ParticipantJoinType.valueOf(joinType));
                participant.setParticipantName(participantName);
                if (StringUtils.isEmpty(serialNumber)) {
                    User user = userMapper.selectByPrimaryKey(userId);

                    // User and dept info.
                    UserDeptSearch udSearch = new UserDeptSearch();
                    udSearch.setUserId(userId);
                    UserDept userDeptCom = userDeptMapper.selectBySearchCondition(udSearch);
                    Department userDep = depMapper.selectByPrimaryKey(userDeptCom.getDeptId());

                    participant.setAppShowInfo(user.getUsername(), "(" + user.getTitle() + ") " + userDep.getDeptName());
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

            if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
                this.notificationService.sendErrorResponseWithDesc(participantPrivatetId, request.getId(),
                        null, errCode);

                log.error("join room:{} failed. errCode:{} message:{}", sessionId, errCode.getCode(), errCode.getMessage());
                if (isModerator(role)) {
                    sessionManager.cleanCacheCollections(sessionId);
                    cleanSession(sessionId, rpcConnection.getParticipantPrivateId(), false, EndReason.forceCloseSessionByUser);
                }
            } else {
                if (!Objects.isNull(rpcConnection.getSerialNumber()) ) {
                    cacheManage.setDeviceStatus(rpcConnection.getSerialNumber(), DeviceStatus.meeting.name());
                }
            }
        } catch (Exception e) {
            log.error("Unknown error e:{}", e);
            if (isModerator(role)) {
                sessionManager.cleanCacheCollections(sessionId);
            }
        }
    }

}
