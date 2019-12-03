package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class StopPtzControlHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String  connectionId = getStringParam(request, ProtocolElements.STOP_PTZ_CONTROL_CONNECTIONID_PARM);
            String serialNumber = lookingDevice(rpcConnection,connectionId);
        if (Objects.isNull(serialNumber)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        JsonObject notifyResult = new JsonObject();
        notifyResult.addProperty(ProtocolElements.STOP_PTZ_CONTROL_SERIAL_NUMBER_PARM, serialNumber);

        sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(p ->{
            if(p.getUserId().equals(rpcConnection.getUserId())) {
                notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.START_PTZ_CONTROL_METHOD, notifyResult);
            }
        });

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
