package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
@Slf4j
@Service
public class SwapPartWindowHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sourceConnectionId = getStringParam(request, ProtocolElements.SWAP_PART_WINDOW_SOURCE_CONNECTION_ID_PAPM);
        String targetConnectionId = getStringParam(request, ProtocolElements.SWAP_PART_WINDOW_TARGET_CONNECTION_ID_PAPM);

        // verify current user role
        io.openvidu.server.core.Session conferenceSession = this.sessionManager.getSession(rpcConnection.getSessionId());
        if (!Objects.isNull(conferenceSession)) {
            if (!OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(rpcConnection.getSessionId(),
                    rpcConnection.getParticipantPrivateId()).getRole())) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.PERMISSION_LIMITED);
                return;

            }
        }

        // change conference layout
        conferenceSession.replacePartOrderInConference(sourceConnectionId, targetConnectionId);
        // json RPC notify KMS layout changed.
        conferenceSession.invokeKmsConferenceLayout();

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

    }
}
