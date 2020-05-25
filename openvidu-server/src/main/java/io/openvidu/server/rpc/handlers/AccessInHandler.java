package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${request.expired-duration}")
    private long reqExpiredDuration;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String uuid = getStringParam(request, ProtocolElements.ACCESS_IN_UUID_PARAM);
        String token = getStringParam(request, ProtocolElements.ACCESS_IN_TOKEN_PARAM);
        String deviceSerialNumber = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_SERIAL_NUMBER_PARAM);
        String deviceMac = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_MAC_PARAM);
        String deviceVersion = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_DEVICEVERSION_PARAM);
        String deviceModel = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_DEVICEMODEL_PARAM);
        String accessType = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_ACCESSTYPE_PARAM);
        String ability = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_ABILITY_PARAM);
//        String terminalConfig = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_TERMINALCONFIG_PARAM);
        JsonElement terminalConfig = getOptionalParam(request, ProtocolElements.ACCESS_IN_TERMINALCONFIG_PARAM);
        String userTypeStr = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_USERTYPE_PARAM);
        UserType userType = !StringUtils.isEmpty(userTypeStr) ? UserType.valueOf(userTypeStr) : UserType.register;
        String clientType = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_CLIENT_TYPE);

        boolean webLogin = false;
        if (!StringUtils.isEmpty(accessType)) {
            rpcConnection.setAccessType(AccessTypeEnum.valueOf(accessType));
            webLogin = Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web);
            if (!webLogin) {
                if (Math.abs(getLongParam(request, ProtocolElements.ACCESS_IN_CLIENTTIMESTAMP_PARAM) - System.currentTimeMillis()) > reqExpiredDuration) {
                    JsonObject errResp = new JsonObject();
                    errResp.addProperty(ProtocolElements.ACCESS_IN_SERVERTIMESTAMP_PARAM, System.currentTimeMillis());
                    this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(),
                            request.getId(), errResp, ErrorCodeEnum.REQUEST_EXPIRED);
                    return;
                }
            }
        }
        boolean forceLogin = getBooleanParam(request, ProtocolElements.ACCESS_IN_FORCE_LOGIN_PARAM);
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;
        JsonObject object = new JsonObject();
        Device device = null;
        RpcConnection previousRpc = null;
        boolean reconnect = false;
        Long accessInUserId = null;
        do {
            // verify parameters
            if (StringUtils.isEmpty(uuid) || StringUtils.isEmpty(token)) {
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

            if (!webLogin && userInfo.containsKey("serialNumber") && !Objects.equals(deviceSerialNumber, userInfo.get("serialNumber"))) {
                log.error("account:{} related device:{} and SN in request params is :{}", uuid,
                        userInfo.get("serialNumber"), deviceSerialNumber);
                errCode = ErrorCodeEnum.REQUEST_PARAMS_ERROR;
                break;
            }

            accessInUserId = Long.valueOf(String.valueOf(userInfo.get("userId")));
            rpcConnection.setUserType(userType);
            rpcConnection.setUsername(!StringUtils.isEmpty(userInfo.get("username")) ? String.valueOf(userInfo.get("username")) : null);
            rpcConnection.setClientType(clientType);
            rpcConnection.setMacAddr(deviceMac);
            rpcConnection.setUserId(accessInUserId);

            // verify device valid & TODO. check user org and dev org. the dev org must lower than user org. whether refuse and disconnect it.
            if (!StringUtils.isEmpty(deviceSerialNumber)) {
                DeviceSearch search = new DeviceSearch();
                search.setSerialNumber(deviceSerialNumber);
                if (Objects.isNull(deviceVersion)) {
                    errCode = ErrorCodeEnum.REQUEST_PARAMS_ERROR;
                    break;
                }
                if (Objects.isNull(device = deviceMapper.selectBySearchCondition(search))) {
                    errCode = ErrorCodeEnum.DEVICE_NOT_FOUND;
                    break;
                } else {
                    if (!Objects.equals(String.valueOf(userInfo.get("project")), device.getProject())) {
                        errCode = ErrorCodeEnum.PERMISSION_LIMITED;
                        break;
                    }
                }
                if (!Objects.equals(deviceVersion, device.getVersion()) || !Objects.equals(ability, device.getAbility())
                        || !Objects.equals(deviceModel, device.getDeviceModel())) {
                    Device dev = new Device();
                    dev.setSerialNumber(deviceSerialNumber);
                    dev.setVersion(deviceVersion);
                    dev.setAbility(ability);
                    dev.setDeviceModel(deviceModel);
                    deviceMapper.updateBySerialNumberSelective(dev);
                }
                rpcConnection.setDeviceSerailNumber(deviceSerialNumber);
                rpcConnection.setAbility(ability);
                rpcConnection.setTerminalConfig(!Objects.isNull(terminalConfig) ? terminalConfig.getAsJsonObject() : null);
                cacheManage.setDeviceStatus(deviceSerialNumber, DeviceStatus.online.name());
                object.addProperty(ProtocolElements.ACCESS_IN_DEVICE_NAME_PARAM, device.getDeviceName());
            }

            previousRpc = notificationService.getRpcConnections().stream().filter(s -> {
                if (!Objects.equals(rpcConnection, s) && Objects.equals(s.getUserUuid(), uuid) &&
                        Objects.equals(AccessTypeEnum.terminal, s.getAccessType())) {
                    log.info("find same login user:{}, previous connection id:{}", s.getUserUuid(), s.getParticipantPrivateId());
                    log.info("previous connection userUuid:{}, macAddr:{}, userId:{}", s.getUserUuid(), s.getMacAddr(), s.getUserId());
                    return true;
                } else {
                    log.info("not found previous connection belong to the same user:{}, connection id:{}", uuid, s.getParticipantPrivateId());
                    return false;
                }
            }).findFirst().orElse(null);

            if (webLogin) {
                Session session;
                rpcConnection.setUserUuid(uuid);
                if (Objects.isNull(previousRpc) || StringUtils.isEmpty(previousRpc.getSerialNumber()) ) {
                    errCode = ErrorCodeEnum.TERMINAL_MUST_LOGIN_FIRST;
                    break;
                } else {
                    String deviceStatus = cacheManage.getDeviceStatus(previousRpc.getSerialNumber());
                    if (!Objects.equals(DeviceStatus.online.name(), deviceStatus) && !Objects.equals(DeviceStatus.meeting.name(), deviceStatus)) {
                        log.error("Device:{} is offline or upgrading.", previousRpc.getSerialNumber());
                        errCode = ErrorCodeEnum.TERMINAL_MUST_LOGIN_FIRST;
                        break;
                    }
                }

                if (!StringUtils.isEmpty(previousRpc.getSessionId()) && (session = sessionManager.getSession(previousRpc.getSessionId())) != null &&
                        !Objects.isNull(sessionManager.getSession(previousRpc.getSessionId()).getPartByPrivateIdAndStreamType(previousRpc.getParticipantPrivateId(), StreamType.MAJOR))) {
                    if (!sessionManager.getSession(previousRpc.getSessionId()).getPartByPrivateIdAndStreamType(previousRpc.getParticipantPrivateId(),
                            StreamType.MAJOR).getRole().equals(OpenViduRole.MODERATOR)) {
                        errCode = ErrorCodeEnum.TERMINAL_IS_NOT_MODERATOR;
                    } else {
                        Participant thorPart = session.getParticipants().stream().filter(participant ->
                                Objects.equals(OpenViduRole.THOR, participant.getRole())).findAny().orElse(null);
                        if (!Objects.isNull(thorPart)) {
                            if (!forceLogin) {
                                log.info("1####thorPart privateId:{}, role:{}, userId:{}", thorPart.getParticipantPrivateId(), thorPart.getRole().name(), thorPart.getUserId());
                                errCode = ErrorCodeEnum.WEB_MODERATOR_ALREADY_EXIST;
                            } else {
                                // send remote login notify to current terminal
                                notificationService.sendNotification(thorPart.getParticipantPrivateId(), ProtocolElements.REMOTE_LOGIN_NOTIFY_METHOD, new JsonObject());
                                leaveRoomAfterConnClosed(thorPart.getParticipantPrivateId(), EndReason.sessionClosedByServer);
                                notificationService.closeRpcSession(thorPart.getParticipantPrivateId());
                            }
                        }
                    }
                    break;
                } else {
                    RpcConnection currLoginThorConnect = notificationService.getRpcConnections().stream()
                        .filter(rpcConn -> !Objects.equals(rpcConn, rpcConnection) && Objects.equals(AccessTypeEnum.web, rpcConn.getAccessType())
                            && Objects.equals(uuid, rpcConn.getUserUuid())).findAny().orElse(null);
                    if (!Objects.isNull(currLoginThorConnect)) {
                        if (!forceLogin) {
                            log.info("2####currLoginThorConnect privateId:{}, uuid:{}, accessType:{}", currLoginThorConnect.getParticipantPrivateId(), currLoginThorConnect.getUserUuid(), currLoginThorConnect.getAccessType().name());
                            errCode = ErrorCodeEnum.WEB_MODERATOR_ALREADY_EXIST;
                        } else {
                            notificationService.sendNotification(currLoginThorConnect.getParticipantPrivateId(), ProtocolElements.REMOTE_LOGIN_NOTIFY_METHOD, new JsonObject());
                            leaveRoomAfterConnClosed(currLoginThorConnect.getParticipantPrivateId(), EndReason.sessionClosedByServer);
                            notificationService.closeRpcSession(currLoginThorConnect.getParticipantPrivateId());
                        }
                    }
                    break;
                }
            }

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
//                cacheManage.updateReconnectInfo(uuid, previousRpc.getParticipantPrivateId());
                if (!StringUtils.isEmpty(previousRpc.getSessionId())) {
                    Session session = sessionManager.getSession(previousRpc.getSessionId());
                    Participant participant;
                    if (Objects.nonNull(session) && Objects.nonNull(previousRpc.getUserId())
                            && Objects.nonNull(participant = session.getParticipantByUserId(String.valueOf(previousRpc.getUserId())))) {
                        cacheManage.updateReconnectInfo(uuid, participant.getParticipantPrivateId());
                    }
                }
                rpcConnection.setUserUuid(uuid);
                previousRpc.setUserUuid(null);
                log.info("the account:{} now reconnect.", uuid);
                break;
            }

            // RECONNECT AFTER RECONNECT
            /*if (!Objects.isNull(previousRpc) && (Objects.equals(userInfo.get("status"), UserOnlineStatusEnum.reconnect.name()))
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
            }*/

            if (!Objects.isNull(previousRpc)) {
                log.warn("NOT MATCH SINGLE LOGIN either RECONNECT and connection id:{}, userUuid:{}, macAddr:{}, userId:{}",
                        rpcConnection.getParticipantPrivateId(), uuid, rpcConnection.getMacAddr(), rpcConnection.getUserId());
                if (Objects.equals(previousRpc.getMacAddr(), deviceMac) &&
                        !Objects.equals(userInfo.get("status"), UserOnlineStatusEnum.offline.name())) {
                    log.info("the account:{} now reconnect.", uuid);
                    rpcConnection.setReconnected(true);
                    reconnect = true;
                    if (!StringUtils.isEmpty(previousRpc.getSessionId())) {
                        Session session = sessionManager.getSession(previousRpc.getSessionId());
                        Participant participant;
                        if (Objects.nonNull(session) && Objects.nonNull(previousRpc.getUserId())
                                && Objects.nonNull(participant = session.getParticipantByUserId(String.valueOf(previousRpc.getUserId())))) {
                            cacheManage.updateReconnectInfo(uuid, participant.getParticipantPrivateId());
                        }
                    }
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
                if (!Objects.equals(errCode, ErrorCodeEnum.WEB_MODERATOR_ALREADY_EXIST)) {
                    sessionManager.accessOut(rpcConnection);
                }
                return;
            } else {
                if (!forceLogin) {
                    JsonObject result = new JsonObject();
                    result.addProperty(ProtocolElements.ACCESS_IN_MAC_PARAM, !StringUtils.isEmpty(deviceMac) ? deviceMac : "");
                    if (!Objects.isNull(device))
                        result.addProperty(ProtocolElements.ACCESS_IN_DEVICE_NAME_PARAM, device.getDeviceName());
                    notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), result, errCode);
                    /*//---------------------
                    notificationService.closeRpcSession(rpcConnection.getParticipantPrivateId());
                    return;*/
                } else {
                    assert previousRpc != null;
                    try {
                        // send remote login notify to current terminal
                        previousRpc.setUserUuid(null);
                        notificationService.sendNotification(previousRpc.getParticipantPrivateId(), ProtocolElements.REMOTE_LOGIN_NOTIFY_METHOD, new JsonObject());
                    } catch (Exception e) {
                        log.error("Exception when send remoteLoginNotify.", e);
                    }

                    if (!StringUtils.isEmpty(previousRpc.getSessionId())) {
                        // leave room
                        Session session = this.sessionManager.getSession(previousRpc.getSessionId());
                        if (!Objects.isNull(session)) {
                            Participant prePart = session.getPartByPrivateIdAndStreamType(previousRpc.getParticipantPrivateId(), StreamType.MAJOR);
                            if (!Objects.isNull(prePart)) {
                                if (Objects.equals(OpenViduRole.MODERATOR, prePart.getRole())) {    // close room
                                    this.sessionManager.dealSessionClose(previousRpc.getSessionId(), EndReason.closeSessionByModerator);
                                } else {    // leave room
                                    if (Objects.equals(ParticipantShareStatus.on, prePart.getShareStatus())) {
                                        Participant preSharePart = session.getPartByPrivateIdAndStreamType(previousRpc.getParticipantPrivateId(), StreamType.SHARING);
                                        if (!Objects.isNull(preSharePart)) {
                                            this.sessionManager.dealParticipantLeaveRoom(preSharePart, false, null);
                                        }
                                    }
                                    this.sessionManager.dealParticipantLeaveRoom(prePart, true, null);
                                }
                            }
                        }
                    }
                }
            }
        }
        // update user online status in cache
        if (!webLogin) {
            cacheManage.updateDeviceName(uuid, Objects.isNull(device) ? "" : device.getDeviceName());
            cacheManage.updateUserOnlineStatus(uuid, reconnect ? UserOnlineStatusEnum.reconnect : UserOnlineStatusEnum.online);
        }
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), object);

        if (reconnect) {
            String conferenceId = previousRpc.getSessionId();
            String previousRpcConnectId = previousRpc.getParticipantPrivateId();
            rpcConnection.setSessionId(conferenceId);
            if (StringUtils.isEmpty(conferenceId)) {
                log.info("the account:{} previous rpc connect id:{} not in conference when reconnect", uuid, previousRpcConnectId);
                notificationService.closeRpcSession(previousRpcConnectId);
                return;
            }

            // Send user break line notify
            JsonObject params = new JsonObject();
            Participant part = this.sessionManager.getParticipant(previousRpcConnectId, StreamType.MAJOR);
            if (part == null) {
                // maybe can not find participant,because of server is restart
                log.warn("CAN NOT FIND THE PARTICIPANT, the account:{} previous rpc connect id:{} userId:{} in conferenceId:{} " +
                                "when reconnect and then close it.", uuid, previousRpcConnectId, previousRpc.getUserId(), conferenceId);
                notificationService.closeRpcSession(previousRpcConnectId);
                return;
            } else {
                params.addProperty(ProtocolElements.USER_BREAK_LINE_CONNECTION_ID_PARAM,
                        part.getParticipantPublicId());
            }


            Participant preSharingPart = this.sessionManager.getParticipant(previousRpcConnectId, StreamType.SHARING);
            JsonObject notifyObj = new JsonObject();
            boolean endShareNotify = !Objects.isNull(preSharingPart);
            if (endShareNotify) {
                // Send reconnected participant stop publish previous sharing if exists
                notifyObj.addProperty(ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_CONNECTIONID_PARAM,
                        preSharingPart.getParticipantPublicId());
                KurentoSession kurentoSession = (KurentoSession) sessionManager.getSession(conferenceId);
                if (!StringUtils.isEmpty(kurentoSession.compositeService.getShareStreamId()) &&
                        kurentoSession.compositeService.getShareStreamId().contains(preSharingPart.getParticipantPublicId())) {
                    kurentoSession.compositeService.setExistSharing(false);
                    kurentoSession.compositeService.setShareStreamId(null);
                }
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
                if (!Objects.equals(previousRpcConnectId, participant.getParticipantPrivateId())
                        && Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                    RpcConnection rpc = notificationService.getRpcConnection(participant.getParticipantPrivateId());
                    if (!Objects.isNull(rpc)) {
                        if (Objects.equals(cacheManage.getUserInfoByUUID(rpc.getUserUuid()).get("status"),
                                UserOnlineStatusEnum.online.name())) {
                            if (endShareNotify) {
                                notificationService.sendNotification(participant.getParticipantPrivateId(),
                                        ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_METHOD, notifyObj);
                            }

                            if (params.size() > 0) {
                                notificationService.sendNotification(participant.getParticipantPrivateId(),
                                        ProtocolElements.USER_BREAK_LINE_METHOD, params);
                            }

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
