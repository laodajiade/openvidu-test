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
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author geedow
 * @date 2019/11/5 16:49
 */
@Slf4j
@Service
public class RaiseHandHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.RAISE_HAND_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.RAISE_HAND_SOURCE_ID_PARAM);
        sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).changeHandStatus(ParticipantHandStatus.up);

        List<String> notifyClientPrivateIds = sessionManager.getParticipants(sessionId)
                .stream().filter(participant -> Objects.equals(StreamType.MAJOR, participant.getStreamType()))
                .map(Participant::getParticipantPrivateId).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(notifyClientPrivateIds)) {

            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.RAISE_HAND_ROOM_ID_PARAM, sessionId);
            params.addProperty(ProtocolElements.RAISE_HAND_SOURCE_ID_PARAM, sourceId);
            notifyClientPrivateIds.forEach(client ->
                    this.notificationService.sendNotification(client, ProtocolElements.RAISE_HAND_METHOD, params));
        }
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
