package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/**
 * @author chosongi
 * @date 2019/11/5 17:00
 */
@Slf4j
@Service
public class SetRollCallHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_SOURCE_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM);

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        participants.stream().filter(part ->
                targetId.equals(gson.fromJson(part.getClientMetadata(), JsonObject.class).get("clientData").getAsString())
                        && Objects.equals(part.getStreamType(), StreamType.MAJOR))
                .findFirst().get().setHandStatus(ParticipantHandStatus.speaker);

        int raiseHandNum = 0;
        for (Participant p : participants) {
            if (Objects.equals(StreamType.MAJOR, p.getStreamType()) && p.getHandStatus() == ParticipantHandStatus.up) {
                ++raiseHandNum;
            }
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.SET_ROLL_CALL_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, targetId);
        params.addProperty(ProtocolElements.SET_ROLL_CALL_RAISEHAND_NUMBER_PARAM, raiseHandNum);
        participants.forEach(participant ->
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_ROLL_CALL_METHOD, params));

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
