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
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.core.SessionPresetEnum;
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
        if (rpcConnection.getUserUuid().equals("80103600005")) {
            return;
        }
        String streamType = getStringParam(request, ProtocolElements.PUBLISHVIDEO_STREAM_TYPE_PARAM);
        Participant participant;
        if (Objects.isNull(participant = sanityCheckOfSession(rpcConnection, StreamType.valueOf(streamType)))) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNRECOGNIZED_API);
            return;
        }
        // start polling,under the wall can publishVideo
        Session session = sessionManager.getSession(participant.getSessionId());
        if (Objects.nonNull(session)) {
            SessionPreset preset = session.getPresetInfo();
            if (preset.getPollingStatusInRoom().equals(SessionPresetEnum.off)) {
                // check part role
                if (OpenViduRole.NON_PUBLISH_ROLES.contains(participant.getRole())) {
                    notificationService.sendErrorResponseWithDesc(participant.getParticipantPrivateId(), request.getId(),
                            null, ErrorCodeEnum.INVALID_METHOD_CALL);
                    return;
                }
            }

            if (sessionManager.isPublisherInSession(rpcConnection.getSessionId(), participant,preset.getPollingStatusInRoom())) {
                MediaOptions options = sessionManager.generateMediaOptions(request);
                sessionManager.publishVideo(participant, options, request.getId());
            } else {
                log.error("Error: participant {} is not a publisher and role={} and streamType={}", participant.getParticipantPublicId()
                        , participant.getRole(), participant.getStreamType());
                throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                        "Unable to publish video. The user does not have a valid token");
            }
        }



        // deal participant that role changed
        String key;
        if (Objects.nonNull(session)
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
