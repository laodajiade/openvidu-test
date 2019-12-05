package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
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
        boolean automatically = getBooleanParam(request, ProtocolElements.SETCONFERENCELAYOUT_AUTOMATICAlly_PARAM);
        Integer mode = getIntOptionalParam(request, ProtocolElements.SETCONFERENCELAYOUT_MODE_PARAM);
        LayoutModeEnum layoutModeEnum = null;
        if (mode != null) {
            layoutModeEnum = LayoutModeEnum.getLayoutMode(mode);
        }
        if (Objects.isNull(layoutModeEnum)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }
        Participant moderator = sessionManager.getParticipant(rpcConnection.getSessionId(),
                rpcConnection.getParticipantPrivateId(), StreamType.MAJOR);
        // verify current user role
        if (!OpenViduRole.MODERATOR_ROLES.contains(moderator.getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        io.openvidu.server.core.Session conferenceSession = this.sessionManager.getSession(rpcConnection.getSessionId());
        if (!Objects.isNull(conferenceSession)) {
            if (!automatically) {
                boolean layoutModeChanged = !Objects.equals(layoutModeEnum, conferenceSession.getLayoutMode());
                if (!layoutModeChanged) {
                    log.info("session:{} layout not changed.", conferenceSession.getSessionId());
                    this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
                    return;
                }
                conferenceSession.setAutomatically(false);

                conferenceSession.switchLayoutMode(layoutModeEnum);
                conferenceSession.invokeKmsConferenceLayout();
            } else {
                if (!conferenceSession.isAutomatically()) {
                    int size = conferenceSession.getMajorShareMixLinkedArr().size();
                    conferenceSession.setAutomatically(true);
                    conferenceSession.switchLayoutMode(size >= LayoutModeEnum.THIRTEEN.getMode() ?
                            LayoutModeEnum.THIRTEEN : LayoutModeEnum.getLayoutMode(size));
                    String moderatorPublicId = Objects.equals(OpenViduRole.MODERATOR, moderator.getRole()) ?
                            moderator.getParticipantPublicId() : Objects.requireNonNull(conferenceSession.getParticipants().stream()
                            .filter(participant -> Objects.equals(OpenViduRole.MODERATOR, participant.getRole()) &&
                                    Objects.equals(StreamType.MAJOR, participant.getStreamType())).findAny().orElse(null)).getParticipantPublicId();
                    conferenceSession.reorder(moderatorPublicId);
                    conferenceSession.invokeKmsConferenceLayout();
                }
            }

             // broadcast the changes of layout
            JsonObject notifyResult = new JsonObject();
            notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY_MODE_PARAM, conferenceSession.getLayoutMode().getMode());
            notifyResult.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM, conferenceSession.getCurrentPartInMcuLayout());
            notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_AUTOMATICALLY_PARAM, automatically);
            sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(p ->
                    notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, notifyResult));
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
