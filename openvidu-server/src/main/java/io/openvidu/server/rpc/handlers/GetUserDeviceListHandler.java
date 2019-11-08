package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.UserOnlineStatusEnum;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 20:22
 */
@Slf4j
@Service
public class GetUserDeviceListHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Long orgId = getLongParam(request, ProtocolElements.GET_USER_DEVICE_ORGID_PARAM);
        String localSerialNumber = rpcConnection.getSerialNumber();
        Long localUserId = rpcConnection.getUserId();
        Map<String, Long> onlineDeviceList = new HashMap<>();
        Map<Long, String> onlineUserList = new HashMap<>();
        for (RpcConnection c : notificationService.getRpcConnections()) {
            Map userInfo = cacheManage.getUserInfoByUUID(c.getUserUuid());
            if (Objects.isNull(userInfo)) continue;
            String status = String.valueOf(userInfo.get("status"));
            if (Objects.equals(UserOnlineStatusEnum.online.name(), status)) {
                onlineDeviceList.put(c.getSerialNumber(), c.getUserId());
                onlineUserList.put(c.getUserId(), c.getSerialNumber());
                log.info("Status:{}, privateId:{}, userId:{}, serialNumber:{}", status, c.getParticipantPrivateId(), c.getUserId(), c.getSerialNumber());
            }
        }

        JsonObject params = new JsonObject();
        JsonArray userDevList = new JsonArray();
        List<Device> deviceList = deviceManage.getSubDeviceByDeptId(orgId);
        if (!CollectionUtils.isEmpty(deviceList)) {
            deviceList.forEach(device -> {
                // 返回列表中排除自己的设备
                if (device.getSerialNumber().equals(localSerialNumber))
                    return ;

                JsonObject devObj = new JsonObject();
                devObj.addProperty(ProtocolElements.GET_USER_DEVICE_DEVICE_NAME_PARAM, device.getDeviceName());
                devObj.addProperty("appShowName", device.getDeviceName());
                devObj.addProperty("appShowDesc", "(" + device.getDeviceModel() + ")");
                if (onlineDeviceList.containsKey(device.getSerialNumber())) {
                    devObj.addProperty(ProtocolElements.GET_USER_DEVICE_STATUS_PARAM, DeviceStatus.online.name());
                    devObj.addProperty(ProtocolElements.GET_USER_DEVICE_USER_ID_PARAM, Long.valueOf(onlineDeviceList.get(device.getSerialNumber())));
                } else {
                    devObj.addProperty(ProtocolElements.GET_USER_DEVICE_STATUS_PARAM, DeviceStatus.offline.name());
                }

                userDevList.add(devObj);
            });
        }

        List<User> userList = userManage.getSubUserByDeptId(orgId);
        if (!CollectionUtils.isEmpty(userList)) {
            userList.forEach(user -> {
                // 返回列表中排除自己的用户
                if (user.getId().equals(localUserId))
                    return ;

                JsonObject userObj = new JsonObject();
                userObj.addProperty(ProtocolElements.GET_USER_DEVICE_USER_NAME_PARAM, user.getUsername());
                userObj.addProperty("appShowName", user.getUsername() + " (" + user.getTitle() + ")");
                userObj.addProperty("appShowDesc", "ID: " + user.getUuid());
                userObj.addProperty(ProtocolElements.GET_USER_DEVICE_STATUS_PARAM, onlineUserList.containsKey(user.getId()) ?
                        DeviceStatus.online.name() : DeviceStatus.offline.name());
                userObj.addProperty(ProtocolElements.GET_USER_DEVICE_USER_ID_PARAM, user.getId());

                userDevList.add(userObj);
            });
        }

        params.add(ProtocolElements.GET_USER_DEVICE_LIST_PARAM, userDevList);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
    }
}
