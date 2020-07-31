package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.core.MediaOptions;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 17:18
 */
@Slf4j
@Service
public class PublishVideoHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String streamType = getStringParam(request, ProtocolElements.PUBLISHVIDEO_STREAM_TYPE_PARAM);
        String handStatus = getStringOptionalParam(request, ProtocolElements.PUBLISHVIDEO_HAND_STATUS_PARAM);
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, StreamType.valueOf(streamType));
        } catch (OpenViduException e) {
            return;
        }

        // check part role
        if (OpenViduRole.NON_PUBLISH_ROLES.contains(participant.getRole())) {
            notificationService.sendErrorResponseWithDesc(participant.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.INVALID_METHOD_CALL);
            return;
        }

        if (sessionManager.isPublisherInSession(rpcConnection.getSessionId(), participant)) {
            MediaOptions options = sessionManager.generateMediaOptions(request);
            participant.setVideoStatus(options.isVideoActive() ? ParticipantVideoStatus.on : ParticipantVideoStatus.off);
            participant.setMicStatus(options.isAudioActive() ? ParticipantMicStatus.on : ParticipantMicStatus.off);
            sessionManager.publishVideo(participant, options, request.getId());
        } else {
            log.error("Error: participant {} is not a publisher", participant.getParticipantPublicId());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to publish video. The user does not have a valid token");
        }

        // deal participant that role changed
        if (Objects.equals(ParticipantHandStatus.speaker.name(), handStatus)
                && StreamType.MAJOR.name().equals(streamType)) {
            sessionManager.setRollCallInSession(sessionManager.getSession(rpcConnection.getSessionId()), participant);
        }
    }
}
