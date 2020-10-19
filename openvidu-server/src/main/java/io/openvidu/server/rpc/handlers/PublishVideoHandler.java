package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantMicStatus;
import io.openvidu.server.common.enums.ParticipantVideoStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.MediaOptions;
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
 * @date 2019/11/5 17:18
 */
@Slf4j
@Service
public class PublishVideoHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String streamType = getStringParam(request, ProtocolElements.PUBLISHVIDEO_STREAM_TYPE_PARAM);
        Participant participant;
        if (Objects.isNull(participant = sanityCheckOfSession(rpcConnection, StreamType.valueOf(streamType)))) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNRECOGNIZED_API);
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
            participant.changeVideoStatus(options.isVideoActive() ? ParticipantVideoStatus.on : ParticipantVideoStatus.off);
            participant.changeMicStatus(options.isAudioActive() ? ParticipantMicStatus.on : ParticipantMicStatus.off);
            sessionManager.publishVideo(participant, options, request.getId());
        } else {
            log.error("Error: participant {} is not a publisher", participant.getParticipantPublicId());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to publish video. The user does not have a valid token");
        }

        // deal participant that role changed
        String key;
        Session session;
        if (Objects.nonNull(session = sessionManager.getSession(participant.getSessionId()))
                && StreamType.MAJOR.name().equals(streamType)
                && cacheManage.existsConferenceRelativeInfo(key = CacheKeyConstants.getSubscriberSetRollCallKey(session.getSessionId(),
                    session.getStartTime(), participant.getUuid()))) {
            sessionManager.setRollCallInSession(sessionManager.getSession(rpcConnection.getSessionId()), participant);

            cacheManage.delConferenceRelativeKey(key);
        }

        // update recording
        if (session.isRecording.get() && participant.ableToUpdateRecord()) {
            sessionManager.updateRecording(session.getSessionId());
        }
    }
}
