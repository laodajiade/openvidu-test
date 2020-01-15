package io.openvidu.server.rpc.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.DeviceMapper;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author geedow
 * @date 2019/11/5 20:26
 */
@Slf4j
@Service
public class GetGroupInfoHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Long groupId = getLongParam(request, ProtocolElements.GET_GROUP_INFO_GROUPID_PARAM);

        List<UserGroup> userGroups = userGroupMapper.selectListByGroupid(groupId);
        JsonObject resp = new JsonObject();
        JsonArray array = new JsonArray();
        for (int i = 0; i < userGroups.size(); i++) {
            JsonObject object = new JsonObject();
            UserGroup userGroup = userGroups.get(i);
            String deviceName = "TODO.NotImplment";
            String deviceStatus = "offline";

            User user = userMapper.selectByPrimaryKey(userGroup.getUserId());
            UserDevice userDevSearch = new UserDevice();
            userDevSearch.setUserId(userGroup.getUserId());
            UserDevice userDevice = userDeviceMapper.selectByCondition(userDevSearch);
            DeviceSearch devSearch = new DeviceSearch();
            devSearch.setSerialNumber(userDevice.getSerialNumber());
            Device device = deviceMapper.selectBySearchCondition(devSearch);
            deviceName = device.getDeviceName();
            deviceStatus = cacheManage.getDeviceStatus(device.getSerialNumber());
            object.addProperty(ProtocolElements.GET_GROUP_INFO_DEVIDE_NAME_PARAM, deviceName);
            object.addProperty(ProtocolElements.GET_GROUP_INFO_ACCOUNT_PARAM, user.getUuid());
            object.addProperty(ProtocolElements.GET_GROUP_INFO_DEVICE_STATUS_PARAM, deviceStatus);
            array.add(object);
        }
        resp.add(ProtocolElements.GET_GROUP_INFO_GROUPINFO_PARAM, array);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), resp);
    }
}
