package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

@Service(ProtocolElements.UPDATE_PARTICIPANT_PROPERTIES_METHOD)
public class UpdateParticipantProperties extends ExRpcAbstractHandler<JsonObject> {

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        final Participant participant = sanityCheckOfSession(rpcConnection);

        if (params.has("usedRTCMode")) {
            cacheManage.updatePartInfo(rpcConnection.getUserUuid(), "usedRTCMode", params.get("usedRTCMode").getAsString());
            participant.setUsedRTCMode(params.get("usedRTCMode").getAsString());
        }

        return RespResult.ok();
    }
}
