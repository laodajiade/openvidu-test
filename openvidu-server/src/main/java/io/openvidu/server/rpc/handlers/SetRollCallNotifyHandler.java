package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @program: prepaid-platform
 * @description:
 * @author: WuBing
 * @create: 2021-07-14 17:46
 **/
@Slf4j
@Service
public class SetRollCallNotifyHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM);
        String originator = getStringOptionalParam(request, "originator");
        List<JsonObject> roleChange = getJsonObjectListParam(request, "roleChange");
        List<JsonObject> updateParticipantsOrder = getJsonObjectListParam(request, "updateParticipantsOrder");

        Session conferenceSession = sessionManager.getSession(sessionId);
        Participant targetPart = conferenceSession.getParticipantByUUID(targetId).orElseGet(null);

        if (targetPart == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    new JsonObject(), ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            return;
        }
        Participant moderatorPart = conferenceSession.getModeratorPart();
        if (moderatorPart == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    new JsonObject(), ErrorCodeEnum.MODERATOR_NOT_FOUND);
            return;
        }

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        for (Participant participant : participants) {
            if (!Objects.equals(StreamType.MAJOR, participant.getStreamType())) continue;
            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, sessionId);
            params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, targetId);
            this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                    ProtocolElements.SET_ROLL_CALL_METHOD, params);
        }


    }
}
