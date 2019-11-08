package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author geedow
 * @date 2019/11/5 19:49
 */
@Slf4j
@Service
public class RemoveFilterEventListenerHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "removeFilterEventListener");
        } catch (OpenViduException e) {
            return;
        }
        String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
        String eventType = getStringParam(request, ProtocolElements.FILTEREVENTLISTENER_EVENTTYPE_PARAM);
        boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

        // Allow removing a filter event listener if the user is a MODERATOR (owning the
        // stream or other user's stream) or if the user is the owner of the stream
        if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
            try {
                sessionManager.removeFilterEventListener(sessionManager.getSession(rpcConnection.getSessionId()),
                        participant, streamId, eventType);
                this.notificationService.sendResponse(participant.getParticipantPrivateId(), request.getId(),
                        new JsonObject());
            } catch (OpenViduException e) {
                this.notificationService.sendErrorResponse(participant.getParticipantPrivateId(), request.getId(),
                        new JsonObject(), e);
            }
        } else {
            log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to remove filter event listener. The user does not have a valid token");
        }
    }
}
