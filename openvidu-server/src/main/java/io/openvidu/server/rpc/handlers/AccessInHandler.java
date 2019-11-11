package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.enums.UserOnlineStatusEnum;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 14:28
 */
@Slf4j
@Service
public class AccessInHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String uuid = getStringParam(request, ProtocolElements.ACCESS_IN_UUID_PARAM);
        String token = getStringParam(request, ProtocolElements.ACCESS_IN_TOKEN_PARAM);
        String deviceSerialNumber = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_SERIAL_NUMBER_PARAM);
        String deviceMac = getStringParam(request, ProtocolElements.ACCESS_IN_MAC_PARAM);
        boolean forceLogin = getBooleanParam(request, ProtocolElements.ACCESS_IN_FORCE_LOGIN_PARAM);
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;
        Device device = null;
        RpcConnection previousRpc = null;
        boolean reconnect = false;
        Long accessInUserId = null;
        do {
            // verify parameters
            if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(token) ||
                    (StringUtils.isEmpty(deviceSerialNumber) && StringUtils.isEmpty(deviceMac))) {
                errCode = ErrorCodeEnum.REQUEST_PARAMS_ERROR;
                break;
            }

            // verify access token
            Map userInfo = cacheManage.getUserInfoByUUID(uuid);
            if (Objects.isNull(userInfo) || !Objects.equals(token, userInfo.get("token"))) {
                log.warn("local token:{} userInfo:{}", token, userInfo);
                errCode = ErrorCodeEnum.TOKEN_INVALID;
                break;
            }
            accessInUserId = Long.valueOf(String.valueOf(userInfo.get("userId")));
            rpcConnection.setMacAddr(deviceMac);
            rpcConnection.setUserId(accessInUserId);

            // verify device valid & TODO. check user org and dev org. the dev org must lower than user org. whether refuse and disconnect it.
            if (!StringUtils.isEmpty(deviceSerialNumber)) {
                DeviceSearch search = new DeviceSearch();
                search.setSerialNumber(deviceSerialNumber);
                if (Objects.isNull(device = deviceMapper.selectBySearchCondition(search))) {
                    errCode = ErrorCodeEnum.DEVICE_NOT_FOUND;
                    break;
                } else {
                    if (!Objects.equals(String.valueOf(userInfo.get("project")), device.getProject())) {
                        errCode = ErrorCodeEnum.PERMISSION_LIMITED;
                        break;
                    }
                }
                rpcConnection.setDeviceSerailNumber(deviceSerialNumber);
            }

            previousRpc = notificationService.getRpcConnections().stream().filter(s -> {
                if (!Objects.equals(rpcConnection, s) && Objects.equals(s.getUserUuid(), uuid)) {
                    log.info("find same login user:{}, previous connection id:{}, ", uuid, s.getParticipantPrivateId());
                    log.info("previous connection userUuid:{}, macAddr:{}, userId:{}", s.getUserUuid(), s.getMacAddr(), s.getUserId());
                    return true;
                } else {
                    log.info("not found previous connection belong to the same user:{}, connection id:{}", uuid, s.getParticipantPrivateId());
                    return false;
                }
            }).findFirst().orElse(null);

            // SINGLE LOGIN
            if (Objects.equals(userInfo.get("status"), UserOnlineStatusEnum.online.name())) {
				/*previousRpc = notificationService.getRpcConnections().stream().filter(s -> !Objects.equals(rpcConnection, s)
						&& Objects.equals(s.getUserUuid(), uuid)).findFirst().orElse(null);*/
                if (!Objects.isNull(previousRpc) && !Objects.equals(previousRpc.getParticipantPrivateId(),
                        rpcConnection.getParticipantPrivateId()) && !Objects.equals(deviceMac, previousRpc.getMacAddr())) {
                    log.warn("SINGLE LOGIN ==> User:{} already online.", userInfo.get("userUuid"));
                    errCode = ErrorCodeEnum.USER_ALREADY_ONLINE;
                    rpcConnection.setUserUuid(uuid);
                    break;
                }
            }

            // OFFLINE RECONNECT
            if (!Objects.isNull(previousRpc) && (Objects.equals(userInfo.get("status"), UserOnlineStatusEnum.offline.name()))
                    && Objects.equals(previousRpc.getMacAddr(), deviceMac)) {
                reconnect = true;
                rpcConnection.setReconnected(true);
                cacheManage.updateReconnectInfo(uuid, previousRpc.getParticipantPrivateId());
                rpcConnection.setUserUuid(uuid);
                previousRpc.setUserUuid(null);
                log.info("the account:{} now reconnect.", uuid);
                break;
            }

            // RECONNECT AFTER RECONNECT
            if (!Objects.isNull(previousRpc) && (Objects.equals(userInfo.get("status"), UserOnlineStatusEnum.reconnect.name()))
                    && Objects.equals(previousRpc.getMacAddr(), deviceMac)) {
                if (previousRpc.isReconnected()) {
                    log.info("the account:{} now reconnect after reconnect. previous connect id:{} userId:{} sessionId:{}",
                            uuid, previousRpc.getParticipantPrivateId(), previousRpc.getUserId(), previousRpc.getSessionId());
                    previousRpc.setUserUuid(null);
                    sessionManager.accessOut(previousRpc);

                    String firstConnectPrivateId = String.valueOf(userInfo.get("reconnect"));
                    previousRpc = notificationService.getRpcConnection(firstConnectPrivateId);
                }

                if (!Objects.isNull(previousRpc)) {
                    reconnect = true;
                    rpcConnection.setReconnected(true);
                    rpcConnection.setUserUuid(uuid);
                    log.info("the account:{} now reconnect after reconnect. first connect id:{} userId:{} sessionId:{}",
                            uuid, previousRpc.getParticipantPrivateId(), previousRpc.getUserId(), previousRpc.getSessionId());
                    break;
                }

                log.warn("RECONNECT AFTER RECONNECT PREVIOUS RPC IS NULL. maybe server is restart, the account:{} now reconnect after reconnect warning.", uuid);
                errCode = ErrorCodeEnum.SERVER_INTERNAL_ERROR;
                break;
            }

            if (!Objects.isNull(previousRpc)) {
                log.warn("NOT MATCH SINGLE LOGIN either RECONNECT and connection id:{}, userUuid:{}, macAddr:{}, userId:{}",
                        rpcConnection.getParticipantPrivateId(), uuid, rpcConnection.getMacAddr(), rpcConnection.getUserId());
                // TODO. 原有断线重连服务端无法及时捕捉原有链接断开信息，此处根据mac及online状态直接判定为重连
                if (Objects.equals(previousRpc.getMacAddr(), deviceMac) &&
                        Objects.equals(userInfo.get("status"), UserOnlineStatusEnum.online.name())) {
                    log.info("the account:{} now reconnect.", uuid);
                    rpcConnection.setReconnected(true);
                    reconnect = true;
                    cacheManage.updateReconnectInfo(uuid, previousRpc.getParticipantPrivateId());
                    rpcConnection.setUserUuid(uuid);
                    previousRpc.setUserUuid(null);
                    break;
                }
                errCode = ErrorCodeEnum.USER_ALREADY_ONLINE;
                rpcConnection.setUserUuid(uuid);
                break;
            }

            rpcConnection.setUserUuid(uuid);
            log.info("NORMAL METHOD CALL ==> rpcConnection userUuid:{}, macAddr:{}, userId:{}",
                    rpcConnection.getUserUuid(), rpcConnection.getMacAddr(), rpcConnection.getUserId());
        } while (false);

        if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
            log.warn("AccessIn Warning. privateId:{}, errCode:{}", rpcConnection.getParticipantPrivateId(), errCode.name());
            if (!Objects.equals(errCode, ErrorCodeEnum.USER_ALREADY_ONLINE)) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),null, errCode);
                sessionManager.accessOut(rpcConnection);
                return;
            } else {
                if (!forceLogin) {
                    JsonObject result = new JsonObject();
                    result.addProperty(ProtocolElements.ACCESS_IN_MAC_PARAM, !StringUtils.isEmpty(deviceMac) ? deviceMac : "");
                    if (!Objects.isNull(device))
                        result.addProperty(ProtocolElements.ACCESS_IN_DEVICE_NAME_PARAM, device.getDeviceName());
                    notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), result, errCode);
                    //---------------------
                    notificationService.closeRpcSession(rpcConnection.getParticipantPrivateId());
                    return;
                } else {
                    // send remote login notify to current terminal
                    assert previousRpc != null;
                    notificationService.sendNotification(previousRpc.getParticipantPrivateId(), ProtocolElements.REMOTE_LOGIN_NOTIFY_METHOD, new JsonObject());
                    leaveRoomAfterConnClosed(previousRpc.getParticipantPrivateId(), EndReason.sessionClosedByServer);
                    notificationService.closeRpcSession(previousRpc.getParticipantPrivateId());
                }
            }
        }
        // update user online status in cache
        cacheManage.updateDeviceName(uuid, Objects.isNull(device) ? "" : device.getDeviceName());
        cacheManage.updateUserOnlineStatus(uuid, reconnect ? UserOnlineStatusEnum.reconnect : UserOnlineStatusEnum.online);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        if (reconnect) {
            String conferenceId = previousRpc.getSessionId();
            String previousRpcConnectId = previousRpc.getParticipantPrivateId();
            rpcConnection.setSessionId(conferenceId);
            if (StringUtils.isEmpty(conferenceId)) {
                log.info("the account:{} previous rpc connect id:{} not in conference when reconnect", uuid, previousRpcConnectId);
                return ;
            }

            // Send user break line notify
            JsonObject params = new JsonObject();
            Participant part = this.sessionManager.getParticipant(previousRpcConnectId, StreamType.MAJOR);
            if (part == null) {
                // maybe can not find participant,because of server is restart
                log.warn("CAN NOT FIND THE PARTICIPANT, the account:{} previous rpc connect id:{} userId:{} in conferenceId:{} when reconnect",
                        uuid, previousRpcConnectId, previousRpc.getUserId(), conferenceId);
            }

            params.addProperty(ProtocolElements.USER_BREAK_LINE_CONNECTION_ID_PARAM,
                    part.getParticipantPublicId());

            Participant preSharingPart = this.sessionManager.getParticipant(previousRpcConnectId, StreamType.SHARING);
            JsonObject notifyObj = new JsonObject();
            boolean endShareNotify = !Objects.isNull(preSharingPart);
            if (endShareNotify) {
                // Send reconnected participant stop publish previous sharing if exists
                notifyObj.addProperty(ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_CONNECTIONID_PARAM,
                        preSharingPart.getParticipantPublicId());
            }

            Participant preMajorPart = this.sessionManager.getParticipant(conferenceId, previousRpcConnectId, StreamType.MAJOR);
            boolean endRollNotify = !Objects.isNull(preMajorPart) && Objects.equals(ParticipantHandStatus.speaker,
                    preMajorPart.getHandStatus());
            JsonObject endRollNotifyObj = new JsonObject();
            if (endRollNotify) {
                endRollNotifyObj.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, conferenceId);
                endRollNotifyObj.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, previousRpc.getUserId());

                preMajorPart.setHandStatus(ParticipantHandStatus.endSpeaker);
            }

            this.sessionManager.getParticipants(conferenceId).forEach(participant -> {
                if (!Objects.equals(previousRpcConnectId, participant.getParticipantPrivateId())) {
                    RpcConnection rpc = notificationService.getRpcConnection(participant.getParticipantPrivateId());
                    if (!Objects.isNull(rpc)) {
                        if (Objects.equals(cacheManage.getUserInfoByUUID(rpc.getUserUuid()).get("status"),
                                UserOnlineStatusEnum.online.name())) {
                            if (endShareNotify) {
                                notificationService.sendNotification(participant.getParticipantPrivateId(),
                                        ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_METHOD, notifyObj);
                            }

                            notificationService.sendNotification(participant.getParticipantPrivateId(),
                                    ProtocolElements.USER_BREAK_LINE_METHOD, params);

                            if (endRollNotify) {
                                notificationService.sendNotification(participant.getParticipantPrivateId(),
                                        ProtocolElements.END_ROLL_CALL_METHOD, endRollNotifyObj);
                            }
                        }
                    }
                }
            });
        }
    }

}
