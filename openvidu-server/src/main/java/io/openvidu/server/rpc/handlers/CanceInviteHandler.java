package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2020/3/7 14:13
 */
@Service
public class CanceInviteHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.CANCELINVITE_ROOMID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.CANCELINVITE_SOURCEID_PARAM);
        List<String> targetIds = getStringListParam(request, ProtocolElements.CANCELINVITE_TARGETIDS_PARAM);

        if (CollectionUtils.isEmpty(targetIds) || Objects.isNull(sessionManager.getSession(sessionId))) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        targetIds.forEach(this::cancelInviteCompensation);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(),request.getId(), new JsonObject());

        JsonObject notifyObj = new JsonObject();
        notificationService.getRpcConnections().forEach(rpcConn -> {
            if (targetIds.contains(rpcConn.getUserUuid())) {
                notificationService.sendNotification(rpcConn.getParticipantPrivateId(),
                        ProtocolElements.CANCELINVITE_NOTIFY_METHOD, notifyObj);
            }
        });

    }
}
