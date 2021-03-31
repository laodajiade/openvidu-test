package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author even
 * @date 2021/1/18 17:04
 */
@Slf4j
@Service
public class CallRemoveHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        List<String> uuidList = getStringListParam(request, ProtocolElements.CALL_REMOVE_UUID_PARAM);
        String ruid = getStringParam(request, ProtocolElements.CALL_REMOVE_RUID_PARAM);
        callHistoryMapper.updateCallHistory(ruid, uuidList);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

}
