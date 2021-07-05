package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author even
 * @date 2020/8/19 10:20
 */
@Slf4j
@Service
public class ApplyOpenSpeakerStatusHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.APPLY_OPEN_SPEAKER_STATUS_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.APPLY_OPEN_SPEAKER_STATUS_SOURCE_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.APPLY_OPEN_SPEAKER_STATUS_TARGET_ID_PARAM);
        Session session = sessionManager.getSession(sessionId);
        // verify session valid
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        JsonObject notifyObj = request.getParams().deepCopy();
        notifyObj.addProperty(ProtocolElements.APPLY_OPEN_SPEAKER_STATUS_USERNAME_PARAM, session.getParticipantByUUID(sourceId).get().getUsername());

        List<Participant> moderatorAndThorPart = session.getModeratorAndThorPart();
        for (Participant participant : moderatorAndThorPart) {
            this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                    ProtocolElements.APPLY_OPEN_SPEAKER_STATUS_METHOD, notifyObj);
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
