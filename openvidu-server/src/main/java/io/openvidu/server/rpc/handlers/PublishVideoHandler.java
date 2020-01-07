package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.MediaOptions;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author geedow
 * @date 2019/11/5 17:18
 */
@Slf4j
@Service
public class PublishVideoHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String streamType = getStringParam(request, ProtocolElements.PUBLISHVIDEO_STREAM_TYPE_PARAM);
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, StreamType.valueOf(streamType));
        } catch (OpenViduException e) {
            return;
        }

        if (sessionManager.isPublisherInSession(rpcConnection.getSessionId(), participant)) {
            MediaOptions options = sessionManager.generateMediaOptions(request);
            sessionManager.publishVideo(participant, options, request.getId());
        } else {
            log.error("Error: participant {} is not a publisher", participant.getParticipantPublicId());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to publish video. The user does not have a valid token");
        }
    }
}
