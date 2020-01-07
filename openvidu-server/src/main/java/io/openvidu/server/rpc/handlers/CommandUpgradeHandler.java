package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
@Slf4j
@Service
public class CommandUpgradeHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String serialNumber = getStringParam(request, ProtocolElements.COMMAND_UOGRADE_SERIALNUMBER_PAPM);
        String version = getStringParam(request, ProtocolElements.COMMAND_UOGRADE_VERSION_PAPM);
        String downloadUrl = getStringParam(request, ProtocolElements.COMMAND_UOGRADE_DOWNLOADURL_PAPM);

        String status = cacheManage.getDeviceStatus(serialNumber);
        DeviceSearch search = new DeviceSearch();
        search.setSerialNumber(serialNumber);
        String deviceVersion = deviceMapper.selectBySearchCondition(search).getVersion();
        if (!Objects.equals(status, DeviceStatus.online)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.DEVICE_BUSY);
            return;
        }
        if (!Objects.equals(version, deviceVersion)) {
            notificationService.getRpcConnections().forEach(rpc -> {
                  if (Objects.equals(rpc.getSerialNumber(), serialNumber)) {
                      JsonObject params = new JsonObject();
                      params.addProperty(ProtocolElements.COMMAND_UOGRADE_SERIALNUMBER_PAPM, serialNumber);
                      params.addProperty(ProtocolElements.COMMAND_UOGRADE_VERSION_PAPM, version);
                      params.addProperty(ProtocolElements.COMMAND_UOGRADE_DOWNLOADURL_PAPM, downloadUrl);
                      notificationService.sendNotification(rpcConnection.getParticipantPrivateId(), ProtocolElements.UPGRADE_NOTIFY_METHOD, params);
                  }
            });
        }
        cacheManage.setDeviceStatus(serialNumber, DeviceStatus.upgrading.name());

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
