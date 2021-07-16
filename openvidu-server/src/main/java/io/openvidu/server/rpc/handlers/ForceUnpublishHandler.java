package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
            participant = sanityCheckOfSession(rpcConnection);
        } catch (OpenViduException e) {
            return;
        }

        String streamId = getStringParam(request, ProtocolElements.FORCEUNPUBLISH_STREAMID_PARAM);

        if (enableToOperate(participant, streamId)) {
            if (sessionManager.unpublishStream(sessionManager.getSession(rpcConnection.getSessionId()), streamId,
                    participant, request.getId(), EndReason.forceUnpublishByUser)) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.USER_NOT_STREAMING_ERROR_CODE);
            }

            // broadcast the changes of layout
            Session conferenceSession = sessionManager.getSession(rpcConnection.getSessionId());
            if (Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
                conferenceSession.getParticipants().forEach(part -> {
                    // broadcast the changes of layout
                    this.notificationService.sendNotification(part.getParticipantPrivateId(),
                            ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, conferenceSession.getLayoutNotifyInfo());
                });
            }
        } else {
            log.error("Error: participant {} is neither a moderator nor a thor.", participant.getParticipantPublicId());
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
        }
    }


    private boolean enableToOperate(Participant participant, String streamId) {
        return OpenViduRole.MODERATOR_ROLES.contains(participant.getRole()) || streamId.equals(participant.getPublisherStreamId())
                || Objects.equals(participant.getParticipantPrivateId(),
                sessionManager.getParticipantPrivateIdFromStreamId(participant.getSessionId(), streamId));
    }
}
