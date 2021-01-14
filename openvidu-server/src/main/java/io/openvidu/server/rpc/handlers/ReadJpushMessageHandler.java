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
 * @date 2021/1/14 11:49
 */
@Slf4j
@Service
public class ReadJpushMessageHandler extends RpcAbstractHandler {


    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        List<Long> msgIds = getLongListParam(request, ProtocolElements.READ_JPUSH_MESSAGE_MSGID_PARAM);
        String uuid = getStringParam(request, ProtocolElements.READ_JPUSH_MESSAGE_UUID_PARAM);
        jpushMessageMapper.updateJpushMsg(1, uuid, msgIds);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
