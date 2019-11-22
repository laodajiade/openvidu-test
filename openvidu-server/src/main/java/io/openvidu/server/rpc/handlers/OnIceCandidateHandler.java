package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 17:46
 */
@Service
public class OnIceCandidateHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;

        String endpointName = getStringParam(request, ProtocolElements.ONICECANDIDATE_EPNAME_PARAM);
        String candidate = getStringParam(request, ProtocolElements.ONICECANDIDATE_CANDIDATE_PARAM);
        String sdpMid = getStringParam(request, ProtocolElements.ONICECANDIDATE_SDPMIDPARAM);
        int sdpMLineIndex = getIntParam(request, ProtocolElements.ONICECANDIDATE_SDPMLINEINDEX_PARAM);
        String streamMode = getStringOptionalParam(request, ProtocolElements.ONICECANDIDATE_STREAMMODE_PARAM);
        if (!Objects.isNull(streamMode) &&
                Objects.equals(StreamModeEnum.SFU_SHARING, StreamModeEnum.valueOf(streamMode))) {
            endpointName = endpointName.substring(0, endpointName.indexOf("_"));
        }
        try {
            participant = sanityCheckOfSession(rpcConnection, endpointName, "onIceCandidate");
        } catch (OpenViduException e) {
            return;
        }

        sessionManager.onIceCandidate(participant, endpointName, candidate, sdpMLineIndex, sdpMid, request.getId());
    }
}
