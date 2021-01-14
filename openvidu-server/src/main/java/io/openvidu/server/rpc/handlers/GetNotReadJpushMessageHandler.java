package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author even
 * @date 2021/1/14 12:04
 */
@Slf4j
@Service
public class GetNotReadJpushMessageHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String uuid = getStringParam(request, ProtocolElements.GETNOTREADJPUSHMESSAGE_UUID_PARAM);
        int count = jpushMessageMapper.getNotReadMsgCount(uuid);
        JSONObject respJson = new JSONObject();
        respJson.put("isExist", count != 0);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}
