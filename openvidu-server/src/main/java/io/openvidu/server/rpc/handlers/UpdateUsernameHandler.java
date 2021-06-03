package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.UserDevice;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author chosongi
 * @date 2020/3/4 17:00
 */
@Slf4j
@Service
public class UpdateUsernameHandler extends RpcAbstractHandler {

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

            userManage.updateUserInfo(update);
        } catch (Exception e) {
            log.info("update name error id{},name{},error info", rpcConnection.getUserId(), username, e.getMessage());
        }

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
