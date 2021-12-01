package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.VoiceMode;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author chosongi
 * @date 2020/8/6 17:58
 */
@Service
public class SwitchVoiceModeHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        VoiceMode operation = VoiceMode.valueOf(getStringParam(request, ProtocolElements.SWITCHVOICEMODE_OPERATION_PARAM));
        Participant participant;
        participant = sanityCheckOfSession(rpcConnection);

        if (Objects.isNull(operation) || participant.getVoiceMode().equals(operation)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        sessionManager.switchVoiceMode(participant, operation);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        // send notify
        JsonObject notifyObj = request.getParams().deepCopy();
        notifyObj.addProperty(ProtocolElements.SWITCH_VOICE_MODE_UUID_PARAM, participant.getUuid());
        notificationService.sendBatchNotificationConcurrent(sessionManager.getSession(rpcConnection.getSessionId()).getParticipants(), ProtocolElements.SWITCHVOICEMODE_NOTIFY_METHOD, notifyObj);
    }
}
