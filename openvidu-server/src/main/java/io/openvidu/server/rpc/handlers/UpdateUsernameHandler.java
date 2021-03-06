package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.client.RtcUserClient;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.UserDevice;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author chosongi
 * @date 2020/3/4 17:00
 */
@Slf4j
@Service
public class UpdateUsernameHandler extends RpcAbstractHandler {

    @Autowired
    private RtcUserClient rtcUserClient;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String username = getStringParam(request, ProtocolElements.UPDATEUSERNAME_USERNAME_PARAM);

        User user = userManage.getUserByUserId(rpcConnection.getUserId());
        if (user == null) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_EXIST);
            return;
        }

        Map<String, Object> map = new HashMap<>();
        try {
            switch (user.getType()) {
                case 1:
                    UserDevice userDevice = new UserDevice();
                    userDevice.setUserId(user.getId());
                    UserDevice userDeviceInfo = userDeviceMapper.selectByCondition(userDevice);
                    map.put("serialNumber", userDeviceInfo.getSerialNumber());
                    map.put("deviceName", username);
                    deviceMapper.updateDeviceName(map);
                    break;
                case 2:
                    map.put("sipName", username);
                    map.put("uuid", rpcConnection.getUserUuid());
                    userMapper.updateSip(map);
                    break;
                default:
                    break;
            }

            User update = new User();
            update.setId(rpcConnection.getUserId());
            update.setUsername(username);
            rpcConnection.setUsername(username);
            rtcUserClient.updateRpcConnection(rpcConnection);
            userManage.updateUserInfo(update);
        } catch (Exception e) {
            log.info("update name error id {},name {},error info", rpcConnection.getUserId(), username, e);
        }

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

    public void updateDeviceInfo(String id, String devNum, String devName) {
        Optional<RpcConnection> first = notificationService.getRpcConnections().stream().filter(x -> Objects.equals(x.getSerialNumber(), devNum)).findFirst();
        if (first.isPresent()) {
            RpcConnection rpcConnection = first.get();
            rpcConnection.setUsername(devName);
            rtcUserClient.updateRpcConnection(rpcConnection);
        }
    }
}
