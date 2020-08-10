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

import java.util.Set;

/**
 * @author geedow
 * @date 2019/11/5 17:05
 */
@Service
public class ReplaceRollCallHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_ROOM_ID_PARAM);
        String endTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_END_TARGET_ID_PARAM);
        String startTargetId = getStringParam(request, ProtocolElements.REPLACE_ROLL_CALL_START_TARGET_ID_PARAM);

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        participants.forEach(participant -> {
            if (StreamType.MAJOR.equals(participant.getStreamType())) {
                if (endTargetId.equals(participant.getUuid())) {
                    participant.changeHandStatus(ParticipantHandStatus.down);
                }

                if (startTargetId.equals(participant.getUuid())) {
                    participant.changeHandStatus(ParticipantHandStatus.speaker);
                }
                this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                        ProtocolElements.REPLACE_ROLL_CALL_METHOD, request.getParams());
            }
        });

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
