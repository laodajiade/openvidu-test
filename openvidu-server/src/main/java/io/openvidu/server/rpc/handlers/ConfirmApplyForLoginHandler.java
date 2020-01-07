package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 16:14
 */

@Slf4j
@Service
public class ConfirmApplyForLoginHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        boolean accept = getBooleanParam(request, ProtocolElements.CONFIRM_APPLY_FOR_LOGIN_ACCEPT_PARAM);
        String applicantSessionId = getStringOptionalParam(request, ProtocolElements.CONFIRM_APPLY_FOR_LOGIN_APPLICANT_SESSION_ID_PARAM);

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        if (!Objects.isNull(notificationService.getRpcConnection(applicantSessionId))) {
            JsonObject param = new JsonObject();
            param.addProperty("loginAllowable", accept);
            notificationService.sendNotification(applicantSessionId, ProtocolElements.RESULT_OF_LOGIN_APPLY_NOTIFY, param);
            if (accept) {
                sessionManager.accessOut(rpcConnection);
            }
        }
    }
}
