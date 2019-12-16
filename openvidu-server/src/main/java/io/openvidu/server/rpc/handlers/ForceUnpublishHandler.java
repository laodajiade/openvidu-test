package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

/**
 * @author geedow
 * @date 2019/11/5 19:35
 */
@Slf4j
@Service
public class ForceUnpublishHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "forceUnpublish");
        } catch (OpenViduException e) {
            return;
        }

        if (OpenViduRole.MODERATOR_ROLES.contains(participant.getRole())) {
            String streamId = getStringParam(request, ProtocolElements.FORCEUNPUBLISH_STREAMID_PARAM);
            if (sessionManager.unpublishStream(sessionManager.getSession(rpcConnection.getSessionId()), streamId,
                    participant, request.getId(), EndReason.forceUnpublishByUser)) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.USER_NOT_STREAMING_ERROR_CODE);
            }

            // broadcast the changes of layout
            Session conferenceSession = sessionManager.getSession(rpcConnection.getSessionId());
            JsonObject notifyResult = new JsonObject();
            notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_AUTOMATICALLY_PARAM, conferenceSession.isAutomatically());
            notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY_MODE_PARAM, conferenceSession.getLayoutMode().getMode());
            notifyResult.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM, conferenceSession.getCurrentPartInMcuLayout());

            conferenceSession.getParticipants().forEach(part -> {
                // broadcast the changes of layout
                this.notificationService.sendNotification(part.getParticipantPrivateId(), ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, notifyResult);
            });
        } else {
            log.error("Error: participant {} is neither a moderator nor a thor.", participant.getParticipantPublicId());
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
        }
    }
}
