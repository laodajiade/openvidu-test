package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author chosongi
 * @date 2019/11/5 19:42
 */
@Slf4j
@Service
public class ExecFilterMethodHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "execFilterMethod");
        } catch (OpenViduException e) {
            return;
        }
        String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
        String filterMethod = getStringParam(request, ProtocolElements.FILTER_METHOD_PARAM);
        JsonObject filterParams = new JsonParser().parse(getStringParam(request, ProtocolElements.FILTER_PARAMS_PARAM))
                .getAsJsonObject();
        boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

        // Allow executing filter method if the user is a MODERATOR (owning the stream
        // or other user's stream) or if the user is the owner of the stream
        if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
            Participant moderator = isModerator ? participant : null;
            sessionManager.execFilterMethod(sessionManager.getSession(rpcConnection.getSessionId()), streamId,
                    filterMethod, filterParams, moderator, request.getId(), "execFilterMethod");
        } else {
            log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to execute filter method. The user does not have a valid token");
        }
    }
}
