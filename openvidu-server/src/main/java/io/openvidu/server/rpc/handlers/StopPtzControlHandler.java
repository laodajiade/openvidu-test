package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StopPtzControlHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
            String  serialNumber = getStringParam(request, ProtocolElements.STOP_PTZ_CONTROL_SERIAL_NUMBER_PARM);

        JsonObject notifyResult = new JsonObject();
        notifyResult.addProperty(ProtocolElements.START_PTZ_CONTROL_SERIAL_NUMBER_PARM, serialNumber);

        sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(p ->{
            if(p.getUserId().equals(rpcConnection.getUserId())) {
                notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.START_PTZ_CONTROL_METHOD, notifyResult);
            }
        });

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());


    }
}
