package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author chosongi
 * @date 2020/3/4 17:00
 */
@Slf4j
@Service
public class UpdateUsernameHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String username = getStringParam(request, ProtocolElements.UPDATEUSERNAME_USERNAME_PARAM);

        User update = new User();
        update.setId(rpcConnection.getUserId());
        update.setUsername(username);
        rpcConnection.setUsername(username);

        userManage.updateUserInfo(update);

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
