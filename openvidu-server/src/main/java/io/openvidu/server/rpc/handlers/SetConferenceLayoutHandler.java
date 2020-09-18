package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;

import static io.openvidu.server.common.enums.ConferenceModeEnum.*;

@Slf4j
@Service
public class SetConferenceLayoutHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        io.openvidu.server.core.Session conferenceSession = this.sessionManager.getSession(rpcConnection.getSessionId());

        boolean automatically = getBooleanParam(request, ProtocolElements.SETCONFERENCELAYOUT_AUTOMATICAlly_PARAM);
        Integer mode = getIntOptionalParam(request, ProtocolElements.SETCONFERENCELAYOUT_MODE_PARAM);
        LayoutModeEnum layoutModeEnum = null;
        if (mode != null) {
            layoutModeEnum = LayoutModeEnum.getLayoutMode(mode);
            if (Objects.isNull(layoutModeEnum)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
                return;
            }
        }
        Participant moderator = sessionManager.getParticipant(rpcConnection.getSessionId(),
                rpcConnection.getParticipantPrivateId(), StreamType.MAJOR);
        // verify current user role
        if (!OpenViduRole.MODERATOR_ROLES.contains(moderator.getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        if (!Objects.isNull(conferenceSession)) {
            switch (conferenceSession.getConferenceMode()) {
                case MCU:
                    if (!automatically) {
                        conferenceSession.setAutomatically(false);
                        boolean layoutModeChanged = !Objects.equals(layoutModeEnum, conferenceSession.getLayoutMode());
                        if (!layoutModeChanged) {
                            log.info("session:{} layout not changed.", conferenceSession.getSessionId());
                            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
                            return;
                        }

                        conferenceSession.switchLayoutMode(layoutModeEnum);
                        conferenceSession.invokeKmsConferenceLayout();
                    } else {
                        if (!conferenceSession.isAutomatically()) {
                            conferenceSession.setAutomatically(true);
                            int size = conferenceSession.getMajorShareMixLinkedArr().size();
                            conferenceSession.switchLayoutMode(size >= LayoutModeEnum.THIRTEEN.getMode() ?
                                    LayoutModeEnum.THIRTEEN : LayoutModeEnum.getLayoutMode(size));
                        }
                        String moderatorPublicId = null, speakerId = null;
                        Set<Participant> participants = conferenceSession.getParticipants();
                        for (Participant participant : participants) {
                            if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
                                speakerId = participant.getParticipantPublicId();
                                break;
                            }
                            if (Objects.equals(OpenViduRole.MODERATOR, participant.getRole()) &&
                                    Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                                moderatorPublicId = participant.getParticipantPublicId();
                            }
                        }
                        conferenceSession.reorder(!StringUtils.isEmpty(speakerId) ? speakerId : moderatorPublicId);
                        conferenceSession.invokeKmsConferenceLayout();
                    }

                    // broadcast the changes of layout
                    sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(p -> {
                        if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) return;
                        notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY,
                                conferenceSession.getLayoutNotifyInfo());
                    });
                    break;
                case SFU:
                    conferenceSession.setAutomatically(automatically);
                    if (Objects.nonNull(mode)) {
                        conferenceSession.setLayoutMode(layoutModeEnum);
                    }
                    sessionManager.getSession(rpcConnection.getSessionId()).getParticipants().forEach(p -> {
                        if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) return;
                        notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY,
                                request.getParams());
                    });
                    break;
                default:
            }
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
