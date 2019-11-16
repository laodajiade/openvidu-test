package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.LayoutChangeTypeEnum;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Slf4j
@Service
public class SetConferenceLayoutHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        LayoutModeEnum layoutModeEnum = LayoutModeEnum.getLayoutMode(getIntParam(request,
                ProtocolElements.SETCONFERENCELAYOUT_MODE_PARAM));
        JsonObject replaceInfo = getParam(request, ProtocolElements.SETCONFERENCELAYOUT_REPLACEINFO_PARAM).getAsJsonObject();
        String target = replaceInfo.get(ProtocolElements.SETCONFERENCELAYOUT_TARGET_PARAM).getAsString();
        String replaceMent = replaceInfo.get(ProtocolElements.SETCONFERENCELAYOUT_REPLACEMENT_PARAM).getAsString();
        boolean shareInclude = replaceInfo.get(ProtocolElements.SETCONFERENCELAYOUT_SHAREINCLUDE_PARAM).getAsBoolean();
        if (Objects.isNull(layoutModeEnum) || StringUtils.isEmpty(target) || StringUtils.isEmpty(replaceMent)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }


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
            conferenceSession.replacePartLayout(target, replaceMent, shareInclude);

            // broadcast the changes of layout
            JsonObject notifyResult = new JsonObject();
            notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, layoutModeEnum.getMode());
            notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY_PARAM, layoutModeEnum.name());
            notifyResult.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM, conferenceSession.getMajorMixLinkedArr());
            notifyResult.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLISTSHAREINCLUDE_PARAM, conferenceSession.getMajorShareMixLinkedArr());

            sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(p ->
                    notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.MAJORLAYOUTNOTIFY_METHOD, notifyResult));

            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        }
    }
}
