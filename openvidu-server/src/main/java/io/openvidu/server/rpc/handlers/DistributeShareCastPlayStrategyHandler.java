package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author chosongi
 * @date 2019/12/26 17:56
 */
@Slf4j
@Service
public class DistributeShareCastPlayStrategyHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String connectionId = getStringOptionalParam(request, ProtocolElements.DISTRIBUTESHARECASTPLAYSTRATEGY_CONNECTIONID_PARAM);
        String shareCastPlayStrategy = getStringOptionalParam(request, ProtocolElements.DISTRIBUTESHARECASTPLAYSTRATEGY_STRATEGY_PARAM);

        if (StringUtils.isEmpty(connectionId) || StringUtils.isEmpty(shareCastPlayStrategy)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        if (!OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(rpcConnection.getSessionId(),
                        rpcConnection.getParticipantPrivateId(), StreamType.MAJOR).getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        // distribute the strategy to the terminal
        Participant targetPart = sessionManager.getSession(rpcConnection.getSessionId()).getParticipantByPublicId(connectionId);
        this.notificationService.sendNotification(targetPart.getParticipantPrivateId(),
                ProtocolElements.DISTRIBUTESHARECASTPLAYSTRATEGY_NOTIFY, request.getParams());

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
