package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

@Service
public class AdjustResolutionHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String connectionId = getStringParam(request, ProtocolElements.ADJUST_RESOLUTION_CONNECTIONID_PAPM);
        String serialNumber = getStringOptionalParam(request, ProtocolElements.ADJUST_RESOLUTION_SERIALNUMBER_PAPM);
        int resolution = getIntParam(request, ProtocolElements.ADJUST_RESOLUTION_RESOLUTION_PAPM);

        RpcConnection rpc = lookingDevice(rpcConnection, connectionId);
        // verify current user role
        if (!OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(rpcConnection.getSessionId(),
                rpcConnection.getParticipantPrivateId()).getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }
        JsonObject notifyResult = new JsonObject();
        notifyResult.addProperty(ProtocolElements.ADJUST_RESOLUTION_SERIALNUMBER_PAPM, serialNumber);
        notifyResult.addProperty(ProtocolElements.ADJUST_RESOLUTION_CONNECTIONID_PAPM, connectionId);
        notifyResult.addProperty(ProtocolElements.ADJUST_RESOLUTION_RESOLUTION_PAPM, resolution);
        notificationService.sendNotification(rpc.getParticipantPrivateId(), ProtocolElements.ADJUST_RESOLUTION_NOTIFY_METHOD, notifyResult);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
