package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Slf4j
@Service
public class SetConferenceLayoutHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Integer mode = getIntOptionalParam(request, ProtocolElements.SETCONFERENCELAYOUT_MODE_PARAM);
        LayoutModeEnum layoutModeEnum = null;
        if (mode != null) {
            layoutModeEnum = LayoutModeEnum.getLayoutMode(mode);
        }
        boolean automatically = getBooleanParam(request, ProtocolElements.SETCONFERENCELAYOUT_AUTOMATICAlly_PARAM);
        io.openvidu.server.core.Session conferenceSession = this.sessionManager.getSession(rpcConnection.getSessionId());
        if (!automatically) {
            if (Objects.isNull(layoutModeEnum)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
                return;
            }
            // record the room layout info
            if (!Objects.isNull(conferenceSession)) {
                // verify current user role
                if (!OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(rpcConnection.getSessionId(),
                        rpcConnection.getParticipantPrivateId()).getRole())) {
                    this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                            null, ErrorCodeEnum.PERMISSION_LIMITED);
                    return;
                }


                boolean layoutModeChanged = !Objects.equals(layoutModeEnum, conferenceSession.getLayoutMode());
                if (!layoutModeChanged) {
                    this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
                    return;
                }

                conferenceSession.switchLayoutMode(layoutModeEnum);
            }
        }

        // json RPC notify KMS layout changed.
       /* int result = conferenceSession.invokeKmsConferenceLayout();
        if (result != 1) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
            return;
        }*/

        // broadcast the changes of layout
        JsonObject notifyResult = new JsonObject();

        notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY_MODE_PARAM, conferenceSession.getLayoutMode().getMode());

        notifyResult.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM, conferenceSession.getMajorShareMixLinkedArr());

        notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_AUTOMATICALLY_PARAM, automatically);

        sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(p ->
                notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, notifyResult));

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
