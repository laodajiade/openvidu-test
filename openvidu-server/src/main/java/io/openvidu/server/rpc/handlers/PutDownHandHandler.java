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
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;

/**
 * @author geedow
 * @date 2019/11/5 16:57
 */
@Slf4j
@Service
public class PutDownHandHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String targetId;
        String sessionId = getStringParam(request, ProtocolElements.PUT_DOWN_HAND_ROOM_ID_PARAM);
        boolean isControlAll = StringUtils.isEmpty(targetId = getStringOptionalParam(request,
                ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM));
        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        participants.forEach(participant -> {
            if (Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                if (!isControlAll && targetId.equals(participant.getUuid())) {
                    participant.setHandStatus(ParticipantHandStatus.down);
                } else {
                    participant.setHandStatus(ParticipantHandStatus.down);
                }

                this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                        ProtocolElements.PUT_DOWN_HAND_METHOD, request.getParams());
            }
        });

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
