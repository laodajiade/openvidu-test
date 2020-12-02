package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.LocalDateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
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
        String udid = getStringParam(request, ProtocolElements.ACCESS_IN_UDID_PARAM);   // Unique Device Identifier
        String deviceSerialNumber = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_SERIAL_NUMBER_PARAM);
        AccessTypeEnum accessType = AccessTypeEnum.valueOf(getStringParam(request, ProtocolElements.ACCESS_IN_ACCESSTYPE_PARAM));
        String userTypeStr = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_USERTYPE_PARAM);
        UserType userType = !StringUtils.isEmpty(userTypeStr) ? UserType.valueOf(userTypeStr) : UserType.register;
        String clientType;
        TerminalTypeEnum terminalType = !StringUtils.isEmpty(clientType = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_CLIENT_TYPE))
                ? TerminalTypeEnum.valueOf(clientType) : null;
        String deviceVersion = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_DEVICEVERSION_PARAM);
        String ability = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_ABILITY_PARAM);
        String deviceModel = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_DEVICEMODEL_PARAM);
        String mac = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_MAC_PARAM);
        JsonElement terminalConfig = getOptionalParam(request, ProtocolElements.ACCESS_IN_TERMINALCONFIG_PARAM);
        String deviceName = null;
        Map userInfo = null;
        JsonObject object = new JsonObject();
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;

        do {
            // check if request expired
            object.addProperty(ProtocolElements.ACCESS_IN_SERVERTIMESTAMP_PARAM, System.currentTimeMillis());
            if (Math.abs(getLongParam(request, ProtocolElements.ACCESS_IN_CLIENTTIMESTAMP_PARAM) - System.currentTimeMillis()) > reqExpiredDuration) {
                errCode = ErrorCodeEnum.REQUEST_EXPIRED;
                break;
            }

            // check if token valid
            userInfo = cacheManage.getUserInfoByUUID(uuid);
            if (!ErrorCodeEnum.SUCCESS.equals(errCode  = userInfo.isEmpty() ?
                    ErrorCodeEnum.TOKEN_INVALID : (!Objects.equals(token, userInfo.get("token")) ? ErrorCodeEnum.TOKEN_ERROR : errCode))) {
                log.error("request token:{}, cache token info:{}", token, userInfo);
                break;
            } else if (isAdmin(uuid)) {
                break;
            }

            // check HDC required parameter
            if (TerminalTypeEnum.HDC.equals(terminalType)) {
                if (StringUtils.isEmpty(deviceSerialNumber) || !Objects.equals(deviceSerialNumber, userInfo.get("serialNumber"))) {
                    log.error("account:{} related device:{} and SN in request params is :{}", uuid, userInfo.get("serialNumber"), deviceSerialNumber);
                    errCode = ErrorCodeEnum.REQUEST_PARAMS_ERROR;
                    break;
                }

                Device device;
                DeviceSearch search = new DeviceSearch();
                search.setSerialNumber(deviceSerialNumber);
                if (Objects.isNull(device = deviceMapper.selectBySearchCondition(search))) {
                    errCode = ErrorCodeEnum.DEVICE_NOT_FOUND;
                    break;
                }

                // update device info if necessary
                checkDeviceInfoAndUpdate(device, deviceVersion, ability, deviceModel, mac, terminalConfig , rpcConnection);

                // add deviceName into resp info
                deviceName = device.getDeviceName();
            }

            // find account already login before
            RpcConnection previousRpc = notificationService.getRpcConnections().stream().filter(s -> {
                if (!Objects.equals(rpcConnection, s) && Objects.equals(s.getUserUuid(), uuid)
                        && Objects.equals(AccessTypeEnum.terminal, s.getAccessType())) {
                    log.info("find same login user:{}, previous connection id:{}", s.getUserUuid(), s.getParticipantPrivateId());
                    return true;
                }
                return false;
            }).max(Comparator.comparing(RpcConnection::getCreateTime)).orElse(null);

            // check if single login
            if (AccessTypeEnum.terminal.equals(accessType) && Objects.nonNull(previousRpc) && !Objects.equals(previousRpc.getUdid(), udid)) {
                log.error("SINGLE LOGIN ==> User:{} already login and pre privateId:{}. current udid:{}, previous udid:{}",
                        userInfo.get("userUuid"), previousRpc.getParticipantPrivateId(), udid, previousRpc.getUdid());
                // check previous rpc connection ever in the room
                evictPreLoginPart(previousRpc);
            }

            // deal web thor login
            if (AccessTypeEnum.web.equals(accessType)
                    && !ErrorCodeEnum.SUCCESS.equals(errCode = dealWebLogin(request, previousRpc, rpcConnection,uuid))) {
                break;
            }
        } while (false);

        if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(),
                    request.getId(), object, errCode);
            return;
        }

        Long userId = Long.valueOf(String.valueOf(userInfo.get("userId")));
        String project = !StringUtils.isEmpty(userInfo.get("project")) ? String.valueOf(userInfo.get("project")) : CommonConstants.DEFAULT_PROJECT;
        // set necessary into rpc connection
        rpcConnection.setUserUuid(uuid);
        rpcConnection.setUdid(udid);
        rpcConnection.setUserType(userType);
        rpcConnection.setAccessType(accessType);
        rpcConnection.setTerminalType(terminalType);
        rpcConnection.setDeviceSerailNumber(deviceSerialNumber);
        rpcConnection.setUserId(userId);
        Corporation corporation = corporationMapper.selectByCorpProject(project);
        rpcConnection.setCorpId(corporation.getId());
        if (StringUtils.isEmpty(rpcConnection.getSerialNumber())) {
            rpcConnection.setUsername(!StringUtils.isEmpty(userInfo.get("username")) ? String.valueOf(userInfo.get("username")) : null);
        }
        rpcConnection.setProject(project);

        //save cache privateId
        cacheManage.saveAccessInParticipantPrivateId(uuid,rpcConnection.getParticipantPrivateId());

        // update user online status in cache
        if (AccessTypeEnum.terminal.equals(accessType)) {
            cacheManage.updateTerminalStatus(rpcConnection, TerminalStatus.online);
        }
        // record user login history
        if (UserType.register == userType && AccessTypeEnum.terminal == accessType) {
            userManage.saveUserLoginHistroy(UserLoginHistory.builder().userId(userId).uuid(uuid)
                    .terminalType(terminalType.getDesc()).serialNumber(deviceSerialNumber).version(deviceVersion).project(project).build());
        }
        object.addProperty("userName", org.apache.commons.lang.StringUtils.isEmpty(deviceName) ? !StringUtils.isEmpty(userInfo.get("username")) ? String.valueOf(userInfo.get("username")) : null : deviceName);
        object.addProperty("expireDate", DateUtil.getDateFormat(corporation.getExpireDate(),DateUtil.DEFAULT_YEAR_MONTH_DAY));
        object.addProperty("validPeriod", ChronoUnit.DAYS.between(LocalDate.now(), LocalDateUtils.translateFromDate(corporation.getExpireDate())));

        // send resp
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), object);
    }

    private ErrorCodeEnum dealWebLogin(Request<JsonObject> request, RpcConnection previousRpc, RpcConnection rpcConnection,String userUuid) {
        User user = userMapper.selectByUUID(userUuid);
        if (Objects.isNull(user)) {
            return ErrorCodeEnum.USER_NOT_EXIST;
        }
        if (user.getType().equals(1)) {
            // check HDC terminal ever online
            String deviceStatus;
            if (Objects.isNull(previousRpc) || !TerminalTypeEnum.HDC.equals(previousRpc.getTerminalType())
                    || Objects.equals(DeviceStatus.offline.name(), deviceStatus = cacheManage.getDeviceStatus(previousRpc.getSerialNumber()))
                    || Objects.equals(DeviceStatus.upgrading.name(), deviceStatus)) {
                return ErrorCodeEnum.TERMINAL_MUST_LOGIN_FIRST;
            }
        } else if (user.getType().equals(0)) {
            return ErrorCodeEnum.SUCCESS;
        }

        // check HDC terminal necessary conditions
        boolean forceLogin = getBooleanParam(request, ProtocolElements.ACCESS_IN_FORCE_LOGIN_PARAM);
        Session session = !StringUtils.isEmpty(previousRpc.getSessionId()) ? sessionManager.getSession(previousRpc.getSessionId()) : null;
        if (Objects.nonNull(session)) {     // HDC terminal in room
            // return errorCode if HDC in room is not a moderator
            Participant hdcPart = session.getPartByPrivateIdAndStreamType(previousRpc.getParticipantPrivateId(), StreamType.MAJOR);
            if (Objects.nonNull(hdcPart) && !OpenViduRole.MODERATOR.equals(hdcPart.getRole())) {
                return ErrorCodeEnum.TERMINAL_IS_NOT_MODERATOR;
            }

            // check ever exists web THOR
            Participant thorPart = session.getParticipants().stream().filter(participant ->
                    OpenViduRole.THOR.equals(participant.getRole())).findAny().orElse(null);
            if (Objects.nonNull(thorPart)) {
                if (forceLogin) {
                    // send remote login notify to current terminal
                    notificationService.sendNotification(thorPart.getParticipantPrivateId(), ProtocolElements.REMOTE_LOGIN_NOTIFY_METHOD, new JsonObject());
                    leaveRoomAfterConnClosed(thorPart.getParticipantPrivateId(), EndReason.sessionClosedByServer);
                    notificationService.closeRpcSession(thorPart.getParticipantPrivateId());
                } else {
                    log.info("thorPart privateId:{}, role:{}, userId:{} already exists.",
                            thorPart.getParticipantPrivateId(), thorPart.getRole().name(), thorPart.getUserId());
                    return ErrorCodeEnum.WEB_MODERATOR_ALREADY_EXIST;
                }
            }
        } else {    // HDC terminal not in room
            RpcConnection preLoginThorConnect = notificationService.getRpcConnections().stream()
                    .filter(rpcConn -> !Objects.equals(rpcConn, rpcConnection)
                            && Objects.equals(AccessTypeEnum.web, rpcConn.getAccessType())
                            && Objects.equals(userUuid, rpcConn.getUserUuid()))
                    .max(Comparator.comparing(RpcConnection::getCreateTime)).orElse(null);

            if (Objects.nonNull(preLoginThorConnect)) {
                if (forceLogin) {
                    notificationService.sendNotification(preLoginThorConnect.getParticipantPrivateId(), ProtocolElements.REMOTE_LOGIN_NOTIFY_METHOD, new JsonObject());
                    leaveRoomAfterConnClosed(preLoginThorConnect.getParticipantPrivateId(), EndReason.sessionClosedByServer);
                    notificationService.closeRpcSession(preLoginThorConnect.getParticipantPrivateId());
                } else {
                    log.info("preLoginThorConnect privateId:{}, uuid:{}, accessType:{}",
                            preLoginThorConnect.getParticipantPrivateId(), preLoginThorConnect.getUserUuid(), preLoginThorConnect.getAccessType().name());
                    return ErrorCodeEnum.WEB_MODERATOR_ALREADY_EXIST;
                }
            }
        }

        return ErrorCodeEnum.SUCCESS;
    }

    private void evictPreLoginPart(RpcConnection previousRpc) {
        // send remote login notify to previous terminal connection
        notificationService.sendNotification(previousRpc.getParticipantPrivateId(),
                ProtocolElements.REMOTE_LOGIN_NOTIFY_METHOD, new JsonObject());

        Map partInfo = cacheManage.getPartInfo(previousRpc.getUserUuid());
        if (partInfo != null && !partInfo.isEmpty()) {
            // evict the previous parts in room
            sessionManager.evictParticipantByUUID(partInfo.get("roomId").toString(), previousRpc.getUserUuid(),
                    Arrays.asList(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR, EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION));
        }

        if (Objects.nonNull(notificationService.getRpcConnection(previousRpc.getParticipantPrivateId()))) {
            notificationService.closeRpcSession(previousRpc.getParticipantPrivateId());
        }
    }

    private void checkDeviceInfoAndUpdate(Device device, String deviceVersion, String ability, String deviceModel, String mac, JsonElement terminalConfig, RpcConnection rpcConnection) {
        if (!Objects.equals(deviceVersion, device.getVersion())
                || !Objects.equals(ability, device.getAbility())
                || !Objects.equals(deviceModel, device.getDeviceModel())
                || !Objects.equals(mac, device.getDeviceMac())) {
            Device dev = new Device();
            dev.setSerialNumber(device.getSerialNumber());
            dev.setVersion(deviceVersion);
            dev.setAbility(ability);
            dev.setDeviceModel(deviceModel);
            dev.setDeviceMac(mac);

            deviceMapper.updateBySerialNumberSelective(dev);
        }

        rpcConnection.setUsername(device.getDeviceName());
        rpcConnection.setAbility(ability);
        rpcConnection.setTerminalConfig(!Objects.isNull(terminalConfig) ? terminalConfig.getAsJsonObject() : null);
    }

}
