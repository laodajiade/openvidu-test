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
import org.springframework.util.StringUtils;

import java.util.*;

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
        boolean isReconnected = getBooleanParam(request, ProtocolElements.JOINROOM_ISRECONNECTED_PARAM);
        String participantPrivatetId = rpcConnection.getParticipantPrivateId();
        SessionPreset preset = sessionManager.getPresetInfo(sessionId);
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;

        rpcConnection.setReconnected(isReconnected);

        try {
            do {
                // verify room join type
                String joinType = getStringParam(request, ProtocolElements.JOINROOM_TYPE_PARAM);
                if (!rpcConnection.isReconnected() && StreamType.MAJOR.equals(streamType) &&
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
                if (StreamType.MAJOR.equals(streamType) && !Objects.equals(joinType, ParticipantJoinType.invited.name())
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
                if (StreamType.MAJOR.equals(streamType) && AccessTypeEnum.terminal.equals(rpcConnection.getAccessType())) {
                    Map partInfo = cacheManage.getPartInfo(rpcConnection.getUserUuid());
                    if (!partInfo.isEmpty()) {
                        String roomId = partInfo.get("roomId").toString();
                        sessionManager.evictParticipantByUUID(roomId, rpcConnection.getUserUuid(),
                                !sessionId.equals(roomId) ? Arrays.asList(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR, EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION)
                                        : Collections.singletonList(EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION));
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
                    if (StreamType.MAJOR.equals(streamType) && majorParts.size() >= preset.getRoomCapacity()) {
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

                JsonObject clientMetadataObj = gson.fromJson(clientMetadata, JsonObject.class);
                clientMetadataObj.addProperty("account", rpcConnection.getUserUuid());
                // change participant role if web THOR invite the same user
                if (!Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web) && !Objects.isNull(sessionManager.getSession(sessionId))) {
                    Participant thorPart = sessionManager.getSession(sessionId).getParticipants().stream().filter(part -> Objects.equals(OpenViduRole.THOR,
                            part.getRole())).findFirst().orElse(null);
                    if (!Objects.isNull(thorPart) && thorPart.getUserId().equals(clientMetadataObj.get("clientData").getAsLong()) &&
                            !Objects.equals(OpenViduRole.THOR, role) && streamType.equals(StreamType.MAJOR)) {
                        role = OpenViduRole.MODERATOR;
                        clientMetadataObj.addProperty("role", OpenViduRole.MODERATOR.name());
                        log.info("change participant role cause web THOR invite the same userId:{}", rpcConnection.getUserId());
                    }
                }
                clientMetadata = clientMetadataObj.toString();

                // check ever already exits share part
                Session session;
                if (StreamType.SHARING.equals(streamType) && Objects.nonNull(session = sessionManager.getSession(sessionId))
                        && Objects.nonNull(session.getParticipants().stream()
                        .filter(participant -> StreamType.SHARING.equals(participant.getStreamType())).findAny().orElse(null))) {
                    errCode = ErrorCodeEnum.SHARING_ALREADY_EXISTS;
                    break;
                }

                Participant participant;
                if (generateRecorderParticipant) {
                    participant = sessionManager.newRecorderParticipant(sessionId, participantPrivatetId, clientMetadata, role.name(), streamType.name());
                } else {
                    participant = sessionManager.newParticipant(sessionId, participantPrivatetId, clientMetadata,
                            role.name(), streamType.name(), location, platform, participantPrivatetId.substring(0, Math.min(16, participantPrivatetId.length())), rpcConnection.getAbility());
                }

                Long userId = rpcConnection.getUserId();
                String serialNumber = rpcConnection.getSerialNumber();
                String participantName = sessionId + "_" + rpcConnection.getUserUuid() + "_" + streamType.name();
                participant.setPreset(preset);
                participant.setJoinType(ParticipantJoinType.valueOf(joinType));
                participant.setParticipantName(participantName);
                participant.setAbility(rpcConnection.getAbility());
                participant.setUserType(rpcConnection.getUserType());
                participant.setTerminalType(rpcConnection.getTerminalType());
                participant.setUuid(rpcConnection.getUserUuid());
                participant.setUsername(rpcConnection.getUsername());
                if (StringUtils.isEmpty(serialNumber)) {
                    if (UserType.register.equals(participant.getUserType())) {
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
