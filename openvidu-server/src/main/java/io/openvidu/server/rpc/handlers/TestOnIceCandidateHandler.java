package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.IceCandidate;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author geedow
 * @date 2019/11/5 17:46
 */
@Slf4j
@Service
public class TestOnIceCandidateHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;

        String endpointName = getStringParam(request, ProtocolElements.ONICECANDIDATE_EPNAME_PARAM);
        String candidate = getStringParam(request, ProtocolElements.ONICECANDIDATE_CANDIDATE_PARAM);
        String sdpMid = getStringParam(request, ProtocolElements.ONICECANDIDATE_SDPMIDPARAM);
        int sdpMLineIndex = getIntParam(request, ProtocolElements.ONICECANDIDATE_SDPMLINEINDEX_PARAM);


        IceCandidate cand = new IceCandidate(candidate, sdpMid, sdpMLineIndex);

        log.info("receive add iceCandidate ");
        try {
            //TimeUnit.MILLISECONDS.sleep(500);
            TestReceiveVideoFromHandler.receiveEndpoint.addIceCandidate(cand);
        }catch (Exception e){
            log.error("null ? {} ? {} ",TestReceiveVideoFromHandler.receiveEndpoint == null,cand == null);
            e.printStackTrace();
            throw new RuntimeException(e);
        }



    }
}
