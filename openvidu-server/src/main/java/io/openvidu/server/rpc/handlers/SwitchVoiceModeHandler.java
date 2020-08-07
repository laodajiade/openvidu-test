package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
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
        try {
            participant = sanityCheckOfSession(rpcConnection, (Objects.equals(operation, VoiceMode.on) ?
                    "open" : "close") + " voice mode");
        } catch (OpenViduException e) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            return;
        }

        if (participant.getVoiceMode().equals(operation)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        sessionManager.switchVoiceMode(participant, operation);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        // send notify
        JsonObject notifyObj = request.getParams().deepCopy();
        notifyObj.addProperty(ProtocolElements.SWITCHVOICEMODE_SENDERCONNECTIONID_PARAM, participant.getParticipantPublicId());
        sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(part -> {
            if (StreamType.MAJOR.equals(part.getStreamType())) {
                notificationService.sendNotification(part.getParticipantPrivateId(), ProtocolElements.SWITCHVOICEMODE_NOTIFY_METHOD, notifyObj);
            }
        });
    }
}
