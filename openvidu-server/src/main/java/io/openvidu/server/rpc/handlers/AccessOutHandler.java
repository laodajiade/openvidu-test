package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 16:16
 */
@Service
public class AccessOutHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        if (request != null) {
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        }
        if (!Objects.isNull(rpcConnection.getSerialNumber()))
            cacheManage.setDeviceStatus(rpcConnection.getSerialNumber(), DeviceStatus.offline.name());
        sessionManager.accessOut(rpcConnection);
    }
}
