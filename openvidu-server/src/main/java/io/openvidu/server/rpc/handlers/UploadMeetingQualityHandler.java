package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

@Service(ProtocolElements.UPLOAD_MEETING_QUALITY_METHOD)
public class UploadMeetingQualityHandler extends ExRpcAbstractHandler<JsonObject> {
    @Override
    public RespResult<JsonObject> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        params.addProperty("uuid", rpcConnection.getUserUuid());
        params.addProperty("roomId", rpcConnection.getSessionId());
        cacheManage.setMeetingQuality(rpcConnection.getUserUuid(), params);
        return RespResult.ok();
    }
}
