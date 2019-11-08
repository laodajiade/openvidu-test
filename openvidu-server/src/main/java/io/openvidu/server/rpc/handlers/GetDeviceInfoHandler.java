package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author chosongi
 * @date 2019/11/5 20:26
 */
@Slf4j
@Service
public class GetDeviceInfoHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String serialNumber = rpcConnection.getSerialNumber();

        DeviceSearch deviceSearch = new DeviceSearch();
        deviceSearch.setSerialNumber(serialNumber);
        Device device = deviceMapper.selectBySearchCondition(deviceSearch);

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.GET_DEVICE_NANE_PARAM, device.getDeviceName());
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
    }
}
