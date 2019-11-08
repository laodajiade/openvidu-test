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

import java.util.Date;

/**
 * @author chosongi
 * @date 2019/11/5 20:30
 */
@Slf4j
@Service
public class UpdateDeviceInfoHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String deviceName = getStringParam(request, ProtocolElements.UPDATE_DEVICE_NANE_PARAM);
        String serialNumber = rpcConnection.getSerialNumber();

        DeviceSearch deviceSearch = new DeviceSearch();
        deviceSearch.setSerialNumber(serialNumber);
        Device device = deviceMapper.selectBySearchCondition(deviceSearch);

        device.setDeviceName(deviceName);
        device.setUpdateTime(new Date());
        deviceMapper.updateByPrimaryKey(device);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
