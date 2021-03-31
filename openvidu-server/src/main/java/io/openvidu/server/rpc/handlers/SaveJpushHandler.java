package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author even
 * @date 2021/1/26 11:55
 */
@Slf4j
@Service
public class SaveJpushHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String uuid = getStringParam(request, ProtocolElements.SAVE_JPUSH_UUID_PARAM);
        String registrationId = getStringParam(request, ProtocolElements.SAVE_JPUSH_REGISTRATIONID_PARAM);
        cacheManage.updateTokenInfo(uuid, ProtocolElements.SAVE_JPUSH_REGISTRATIONID_PARAM, registrationId);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
