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

/**
 * 2021年4月28日 和SDK沟通过，暂时没有在使用这个接口
 */
@Service
public class ReplaceRollCallHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_ROOM_ID_PARAM);
        String endTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_END_TARGET_ID_PARAM);
        String startTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_START_TARGET_ID_PARAM);
        String originator = getStringParam(request, ProtocolElements.END_ROLL_CALL_ORIGINATOR_ID_PARAM);

        Session session = sessionManager.getSession(sessionId);
        Participant endPart = session.getParticipantByUUID(endTargetId).orElseGet(null);
        Participant startPart = session.getParticipantByUUID(startTargetId).orElseGet(null);
        if (endPart == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    new JsonObject(), ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            return;
        }
        if (startPart == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    new JsonObject(), ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            return;
        }


/*        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        participants.forEach(participant -> {
            if (endTargetId.equals(participant.getUuid())) {
                participant.changeHandStatus(ParticipantHandStatus.down);
            }

            if (startTargetId.equals(participant.getUuid())) {
                participant.changeHandStatus(ParticipantHandStatus.speaker);

            }
            this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                    ProtocolElements.REPLACE_ROLL_CALL_METHOD, request.getParams());
        });*/
        sessionManager.replaceSpeaker(session, endPart, startPart, originator);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        // update recording
        if (sessionManager.getSession(sessionId).ableToUpdateRecord()) {
            sessionManager.updateRecording(sessionId);
        }
    }
}
