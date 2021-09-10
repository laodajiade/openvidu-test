package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.client.RtcUserClient;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.common.pojo.UserLoginHistory;
import io.openvidu.server.core.AppVersion;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.StringUtil;
import io.openvidu.server.utils.ValidPeriodHelper;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author geedow
 * @date 2019/11/5 14:28
 */
@Slf4j
@Service
public class AccessInHandler extends RpcAbstractHandler {

    @Value("${request.expired-duration}")
    private long reqExpiredDuration;

    private static final String DEFAULT_DEVICE_VERSION = "1350";

    @Autowired
    private RtcUserClient rtcUserClient;

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
        String deviceVersion = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_DEVICEVERSION_PARAM, DEFAULT_DEVICE_VERSION);
        String ability = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_ABILITY_PARAM);
        String functionality = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_FUNCTIONALITY_PARAM);
        String deviceModel = getStringParam(request, ProtocolElements.ACCESS_IN_DEVICEMODEL_PARAM);
        String mac = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_MAC_PARAM);
        JsonElement terminalConfig = getOptionalParam(request, ProtocolElements.ACCESS_IN_TERMINALCONFIG_PARAM);
        String nickName = getStringOptionalParam(request, ProtocolElements.ACCESS_IN_NICKNAME_PARAM);
        String deviceName = null;
        Map userInfo = null;
        JsonObject object = new JsonObject();

        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;

        do {
            try {
                deviceVersion = StringUtil.verifiedAndTransformVersion(deviceVersion);
            } catch (IllegalArgumentException e) {
                errCode = ErrorCodeEnum.ILLEGAL_VERSION;
                break;
            }

            if (!StringUtil.compareVersion(AppVersion.SERVER_VERSION, deviceVersion)) {
                errCode = ErrorCodeEnum.VERSION_LOW;
                break;
            }

            // check if request expired
            object.addProperty(ProtocolElements.ACCESS_IN_SERVERTIMESTAMP_PARAM, System.currentTimeMillis());
            if (Math.abs(getLongParam(request, ProtocolElements.ACCESS_IN_CLIENTTIMESTAMP_PARAM) - System.currentTimeMillis()) > reqExpiredDuration) {
                errCode = ErrorCodeEnum.REQUEST_EXPIRED;
                break;
            }

            // check if token valid
            userInfo = cacheManage.getUserInfoByUUID(uuid);
            if (!ErrorCodeEnum.SUCCESS.equals(errCode = userInfo.isEmpty() ?
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
                checkDeviceInfoAndUpdate(device, deviceVersion, ability, functionality, deviceModel, mac, terminalConfig, rpcConnection);

                // add deviceName into resp info
                deviceName = device.getDeviceName();
            }


            // find account already login before
//            RpcConnection previousRpc = notificationService.getRpcConnections().stream().filter(s -> {
//                if (!Objects.equals(rpcConnection, s) && Objects.equals(s.getUserUuid(), uuid)
//                        && Objects.equals(AccessTypeEnum.terminal, s.getAccessType())) {
//                    log.info("find same login user:{}, previous connection id:{}", s.getUserUuid(), s.getParticipantPrivateId());
//                    return true;
//                }
//                return false;
//            }).max(Comparator.comparing(RpcConnection::getCreateTime)).orElse(null);
            List<RpcConnection> previousRpcs = notificationService.getRpcConnectionByUuids(uuid).stream()
                    .filter(s -> {
                        if (!s.getParticipantPrivateId().equals(rpcConnection.getParticipantPrivateId())
                                && Objects.equals(AccessTypeEnum.terminal, s.getAccessType())) {
                            log.info("find same login user:{}, previous connection id:{}", s.getUserUuid(), s.getParticipantPrivateId());
                            return true;
                        }
                        return false;
                    }).collect(Collectors.toList());

            for (RpcConnection previousRpc : previousRpcs) {
                // check if single login
                if (AccessTypeEnum.terminal.equals(accessType)) {
                    if (!Objects.equals(previousRpc.getUdid(), udid)) {
                        log.warn("SINGLE LOGIN ==> User:{} already login and pre privateId:{}. current udid:{}, previous udid:{}",
                                userInfo.get("userUuid"), previousRpc.getParticipantPrivateId(), udid, previousRpc.getUdid());
                        // check previous rpc connection ever in the room
                        evictPreLoginPart(previousRpc);
                    } else {
                        notificationService.closeRpcSession(previousRpc.getParticipantPrivateId());
                    }
                }
            }


        } while (false);

        if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(),
                    request.getId(), object, errCode);
            return;
        }
        if (!StringUtils.isEmpty(clientType)) {
            cacheManage.updateTokenInfo(uuid, ProtocolElements.ACCESS_IN_CLIENT_TYPE, clientType);
        }

        Long userId = Long.valueOf(String.valueOf(userInfo.get("userId")));
        String project = !StringUtils.isEmpty(userInfo.get("project")) ? String.valueOf(userInfo.get("project")) : CommonConstants.DEFAULT_PROJECT;
        // set necessary into rpc connection
        rpcConnection.setUserUuid(uuid);
        rpcConnection.setUdid(udid);
        rpcConnection.setUserType(userType);
        rpcConnection.setAccessType(accessType);
        rpcConnection.setTerminalType(terminalType);
        rpcConnection.setDeviceModel(deviceModel);
        rpcConnection.setSerialNumber(deviceSerialNumber);
        rpcConnection.setUserId(userId);
        rpcConnection.setDeviceVersion(deviceVersion);
        Corporation corporation = corporationMapper.selectByCorpProject(project);
        rpcConnection.setCorpId(UserType.tourist.equals(userType) ? 0L : corporation.getId());
        if (StringUtils.isEmpty(rpcConnection.getSerialNumber())) {
            rpcConnection.setUsername(!StringUtils.isEmpty(userInfo.get("username")) ? String.valueOf(userInfo.get("username")) : "用户");
        }
        if (UserType.tourist.equals(userType)) {
            rpcConnection.setUsername(nickName);
        }
        rpcConnection.setProject(project);

        //save cache privateId
        log.info("access in relation privateId={},uuid={}", rpcConnection.getParticipantPrivateId(), uuid);
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
        if (!UserType.tourist.equals(userType)) {
            object.addProperty("expireDate", corporation.getExpireDate().format(DateTimeFormatter.ofPattern(DateUtil.DEFAULT_YEAR_MONTH_DAY)));
            object.addProperty("validPeriod", ValidPeriodHelper.getBetween(corporation.getExpireDate()));
        }
        rpcConnection.setLoginTime(System.currentTimeMillis());
        rtcUserClient.updateRpcConnection(rpcConnection);
        // send resp
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), object);
    }


    private void evictPreLoginPart(RpcConnection previousRpc) {
        // send remote login notify to previous terminal connection
        notificationService.sendNotification(previousRpc.getParticipantPrivateId(),
                ProtocolElements.REMOTE_LOGIN_NOTIFY_METHOD, new JsonObject());

        Map partInfo = cacheManage.getPartInfo(previousRpc.getUserUuid());
        if (partInfo != null && !partInfo.isEmpty()) {
            // evict the previous parts in room
            sessionManager.evictParticipantByUUIDEx(partInfo.getOrDefault("roomId", "1").toString(), previousRpc.getUserUuid(), Arrays.asList(EvictParticipantStrategy.CLOSE_ROOM_WHEN_EVICT_MODERATOR, EvictParticipantStrategy.CLOSE_WEBSOCKET_CONNECTION),
                    EndReason.forceDisconnectByServer);
        }

//        if (Objects.nonNull(notificationService.getRpcConnection(previousRpc.getParticipantPrivateId()))) {
//            notificationService.closeRpcSession(previousRpc.getParticipantPrivateId());
//        }
    }

    private void checkDeviceInfoAndUpdate(Device device, String deviceVersion, String ability, String functionality,
                                          String deviceModel, String mac, JsonElement terminalConfig, RpcConnection rpcConnection) {
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
        rpcConnection.setFunctionality(functionality);
        rpcConnection.setTerminalConfig(!Objects.isNull(terminalConfig) ? terminalConfig.toString() : null);
    }

}
