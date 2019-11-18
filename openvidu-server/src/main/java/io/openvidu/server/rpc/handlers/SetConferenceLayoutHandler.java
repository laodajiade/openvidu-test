package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.common.layout.LayoutInitHandler;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.KurentoClient;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Service
public class SetConferenceLayoutHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        LayoutModeEnum layoutModeEnum = LayoutModeEnum.getLayoutMode(getIntParam(request,
                ProtocolElements.SETCONFERENCELAYOUT_MODE_PARAM));
        if (Objects.isNull(layoutModeEnum)) {
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

            boolean layoutModeChanged = Objects.equals(layoutModeEnum, conferenceSession.getLayoutMode());
            if (!layoutModeChanged) {
                this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
                return;
            }

            JsonArray layouts = LayoutInitHandler.getLayoutByMode(layoutModeEnum);
            conferenceSession.setLayoutMode(layoutModeEnum);
            conferenceSession.setLayoutCoordinates(layouts);

            // json RPC notify KMS layout changed.
            KurentoSession kurentoSession = (KurentoSession) conferenceSession;
            KurentoClient kurentoClient = kurentoSession.getKms().getKurentoClient();
            Request<JsonObject> kmsRequest = new Request<>();
            JsonObject params = new JsonObject();
            params.addProperty("object", kurentoSession.getPipeline().getId());
            params.addProperty("operation", "setLayout");
            JsonArray layoutInfos = new JsonArray(50);
            for (JsonElement jsonElement : layouts) {
                JsonObject temp = jsonElement.getAsJsonObject();
                JsonObject resultPart = temp.deepCopy();
                KurentoParticipant kurentoParticipant = (KurentoParticipant) conferenceSession.getParticipantByPublicId(temp.get("connectionId").getAsString());
                resultPart.addProperty("object", kurentoParticipant.getPublisher().getWebEndpoint().getId());
                resultPart.addProperty("hasVideo", kurentoParticipant.getPublisherMediaOptions().hasVideo());
                resultPart.addProperty("onlineStatus", String.valueOf(cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid()).get("status")));

                layoutInfos.add(resultPart);
            }
            params.add("layoutInfo", layoutInfos);
            kmsRequest.setMethod("invoke");
            kmsRequest.setParams(params);
            try {
                kurentoClient.sendJsonRpcRequest(kmsRequest);
            } catch (IOException e) {
                log.error("Exception:\n", e);
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
                return;
            }

            // broadcast the changes of layout
            JsonObject notifyResult = new JsonObject();
            notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY_MODE_PARAM, layoutModeEnum.getMode());
            notifyResult.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM, conferenceSession.getMajorShareMixLinkedArr());

            sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(p -> {
                if (!Objects.equals(p.getParticipantPrivateId(), rpcConnection.getParticipantPrivateId()))
                    notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, notifyResult);
            });

            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        }
    }
}
