package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReplaceRollCallHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_ROOM_ID_PARAM);
        String endTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_END_TARGET_ID_PARAM);
        String startTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_START_TARGET_ID_PARAM);
        String originator = getStringParam(request, ProtocolElements.END_ROLL_CALL_ORIGINATOR_ID_PARAM);

        Session session = sessionManager.getSession(sessionId);
        Optional<Participant> endPart = session.getParticipantByUUID(endTargetId);

        Optional<Participant> startPart = session.getParticipantByUUID(startTargetId);
        if (!endPart.isPresent() || !startPart.isPresent()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    new JsonObject(), ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            return;
        }
        sessionManager.replaceSpeaker(session, endPart.get(), startPart.get(), originator);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
