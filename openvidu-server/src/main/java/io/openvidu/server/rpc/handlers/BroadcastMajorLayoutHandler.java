package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.LayoutChangeTypeEnum;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/8 10:58
 */
@Slf4j
@Service
public class BroadcastMajorLayoutHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        /*this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                null, ErrorCodeEnum.UNRECOGNIZED_API);*/
        LayoutModeEnum layoutModeEnum = LayoutModeEnum.getLayoutMode(getIntParam(request,
                ProtocolElements.BROADCASTMAJORLAYOUT_MODE_PARAM));
        if (Objects.isNull(layoutModeEnum)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }
        LayoutChangeTypeEnum changeType = LayoutChangeTypeEnum.valueOf(getStringParam(request,
                ProtocolElements.BROADCASTMAJORLAYOUT_TYPE_PARAM));
        JsonArray layout = (!Objects.isNull(request.getParams()) &&
                request.getParams().has(ProtocolElements.BROADCASTMAJORLAYOUT_LAYOUT_PARAM)) ?
                request.getParams().getAsJsonArray(ProtocolElements.BROADCASTMAJORLAYOUT_LAYOUT_PARAM) : new JsonArray(1);


        // record the room layout info
        io.openvidu.server.core.Session conferenceSession = this.sessionManager.getSession(rpcConnection.getSessionId());
        if (!Objects.isNull(conferenceSession)) {
            // verify current user role
            if (!Objects.equals(OpenViduRole.MODERATOR, sessionManager.getParticipant(rpcConnection.getSessionId(),
                    rpcConnection.getParticipantPrivateId()).getRole())) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.PERMISSION_LIMITED);
                return;
            }

            conferenceSession.setLayoutMode(layoutModeEnum);
            conferenceSession.setLayoutChangeTypeEnum(changeType);
            conferenceSession.setLayoutInfo(layout);

            // broadcast the changes of layout
            JsonObject notifyResult = new JsonObject();
            notifyResult.addProperty(ProtocolElements.MAJORLAYOUTNOTIFY_MODE_PARAM, layoutModeEnum.getMode());
            notifyResult.addProperty(ProtocolElements.MAJORLAYOUTNOTIFY_TYPE_PARAM, changeType.name());
            notifyResult.add(ProtocolElements.MAJORLAYOUTNOTIFY_LAYOUT_PARAM, layout);

            sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(p -> {
                notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.MAJORLAYOUTNOTIFY_METHOD, notifyResult);
            });

            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        }
    }
}
