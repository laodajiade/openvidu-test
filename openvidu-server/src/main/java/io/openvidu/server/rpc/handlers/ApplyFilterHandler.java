package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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
 * @date 2019/11/5 19:38
 */
@Slf4j
@Service
public class ApplyFilterHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant = sanityCheckOfSession(rpcConnection);;


        String filterType = getStringParam(request, ProtocolElements.FILTER_TYPE_PARAM);
        String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
        boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

        // Allow applying filter if the user is a MODERATOR (owning the stream or other
        // user's stream) or if the user is the owner of the stream and has a token
        // configured with this specific filter
        if (isModerator || (this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId))) {
//				&& participant.getToken().getKurentoTokenOptions().isFilterAllowed(filterType))) {
            JsonObject filterOptions;
            try {
                filterOptions = new JsonParser().parse(getStringParam(request, ProtocolElements.FILTER_OPTIONS_PARAM))
                        .getAsJsonObject();
            } catch (JsonSyntaxException e) {
                throw new OpenViduException(OpenViduException.Code.FILTER_NOT_APPLIED_ERROR_CODE,
                        "'options' parameter is not a JSON object: " + e.getMessage());
            }
            Participant moderator = isModerator ? participant : null;
            sessionManager.applyFilter(sessionManager.getSession(rpcConnection.getSessionId()), streamId, filterType,
                    filterOptions, moderator, request.getId(), "applyFilter");
        } else {
            log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to apply filter. The user does not have a valid token");
        }
    }
}
