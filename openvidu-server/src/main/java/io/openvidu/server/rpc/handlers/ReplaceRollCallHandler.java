package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/**
 * @author geedow
 * @date 2019/11/5 17:05
 */
@Service
public class ReplaceRollCallHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        // TODO, Fixme. Maybe have good way to do it. for example: add a roll call type.
        String sessionId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_SOURCE_ID_PARAM);
        String endTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_END_TARGET_ID_PARAM);
        String startTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_START_TARGET_ID_PARAM);

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        participants.stream().filter(part ->
                endTargetId.equals(gson.fromJson(part.getClientMetadata(), JsonObject.class).get("clientData").getAsString()))
                .findFirst().get().setHandStatus(ParticipantHandStatus.down);

        participants.stream().filter(part ->
                startTargetId.equals(gson.fromJson(part.getClientMetadata(), JsonObject.class).get("clientData").getAsString()))
                .findFirst().get().setHandStatus(ParticipantHandStatus.speaker);

        int raiseHandNum = 0;
        for (Participant p : sessionManager.getParticipants(sessionId)) {
            if (p.getHandStatus() == ParticipantHandStatus.up) {
                ++raiseHandNum;
            }
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_ROOM_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_END_TARGET_ID_PARAM, endTargetId);
        params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_START_TARGET_ID_PARAM, startTargetId);
        params.addProperty(ProtocolElements.REPLACE_ROLL_CALL_RAISEHAND_NUMBER_PARAM, raiseHandNum);
        participants.forEach(participant -> {
            if (!Objects.equals(StreamType.MAJOR, participant.getStreamType())) return;
            this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.REPLACE_ROLL_CALL_METHOD, params);
        });

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
