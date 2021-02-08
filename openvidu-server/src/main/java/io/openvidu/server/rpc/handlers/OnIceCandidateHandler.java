package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author geedow
 * @date 2019/11/5 17:46
 */
@Slf4j
@Service
public class OnIceCandidateHandler extends RpcAbstractHandler {
    @Autowired
    TestOnIceCandidateHandler testOnIceCandidateHandler;
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {

        Participant participant;

        String endpointName = getStringParam(request, ProtocolElements.ONICECANDIDATE_EPNAME_PARAM);
        String candidate = getStringParam(request, ProtocolElements.ONICECANDIDATE_CANDIDATE_PARAM);
        String sdpMid = getStringParam(request, ProtocolElements.ONICECANDIDATE_SDPMIDPARAM);
        int sdpMLineIndex = getIntParam(request, ProtocolElements.ONICECANDIDATE_SDPMLINEINDEX_PARAM);
        try {
            participant = sanityCheckOfSession(rpcConnection, endpointName, "onIceCandidate");
        } catch (OpenViduException e) {
            return;
        }
        log.info("testOnIceCandidateHandler 33333333333333 {}, {}", rpcConnection.getUserUuid(),participant.getUuid());
        if (rpcConnection.getUserUuid().equals("80103600005") && participant.getUuid().equals("80103600005")) {
            try {
                log.info("80103600005 testOnIceCandidateHandler");
                testOnIceCandidateHandler.handRpcRequest(rpcConnection, request);

            } catch (Exception e) {
                e.printStackTrace();
            }

            //return;
        } else {
            log.info("testOnIceCandidateHandler 2222222222 {}", participant.getUuid());
        }

        sessionManager.onIceCandidate(participant, endpointName, candidate, sdpMLineIndex, sdpMid, request.getId());
    }
}
