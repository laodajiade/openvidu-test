package io.openvidu.server.rpc.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author geedow
 * @date 2019/11/5 20:26
 */
@Slf4j
@Service
public class GetDeviceInfoHandler extends RpcAbstractHandler {

    @Value("${device.version}")
    private String upgradeVersion;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String version,desc;
        String serialNumber = rpcConnection.getSerialNumber();
        JsonArray jsonArray = new JsonArray();
        JsonArray versionArray = new Gson().fromJson(upgradeVersion, JsonArray.class);
        for (JsonElement element : versionArray) {
            JsonObject jsonObject = new JsonObject();
            JsonObject item = element.getAsJsonObject();
            version = item.get("version").getAsString();
            desc = item.get("desc").getAsString();
            jsonObject.addProperty(ProtocolElements.GET_DEVICE_VERSION_PARAM, version);
            jsonObject.addProperty(ProtocolElements.GET_DEVICE_DESC_PARAM, desc);
            jsonArray.add(jsonObject);
        }
        DeviceSearch deviceSearch = new DeviceSearch();
        deviceSearch.setSerialNumber(serialNumber);
        Device device = deviceMapper.selectBySearchCondition(deviceSearch);

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.GET_DEVICE_NANE_PARAM, device.getDeviceName());
        params.addProperty(ProtocolElements.GET_DEVICE_DEVCURVERSION_PARAM, device.getVersion());
        params.addProperty(ProtocolElements.GET_DEVICE_STATUS_PARAM, cacheManage.getDeviceStatus(device.getDeviceName()));
        params.add(ProtocolElements.GET_DEVICE_VERUPGRADEAVAILABLE_PARAM, jsonArray);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
    }
}
