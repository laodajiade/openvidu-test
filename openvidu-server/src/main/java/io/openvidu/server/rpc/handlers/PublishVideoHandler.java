package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.enums.TerminalTypeEnum;
import io.openvidu.server.core.*;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

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


        Session session = sessionManager.getSession(rpcConnection.getSessionId());
        if (session == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNRECOGNIZED_API);
            return;
        }
        Optional<Participant> participantOptional = session.getParticipantByUUID(rpcConnection.getUserUuid());
        if (!participantOptional.isPresent()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNRECOGNIZED_API);
            return;
        }

        Participant participant = participantOptional.get();

        // start polling,under the wall can publishVideo
        SessionPreset preset = session.getPresetInfo();
        if (preset.getPollingStatusInRoom().equals(SessionPresetEnum.off)) {
            // check part role
            if (OpenViduRole.NON_PUBLISH_ROLES.contains(participant.getRole()) && participant.getTerminalType() != TerminalTypeEnum.S) {
                notificationService.sendErrorResponseWithDesc(participant.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.INVALID_METHOD_CALL);
                return;
            }
        }

        if (sessionManager.isPublisherInSession(rpcConnection.getSessionId(), participant, preset.getPollingStatusInRoom())) {
            MediaOptions options = sessionManager.generateMediaOptions(request);
            sessionManager.publishVideo(participant, options, request.getId(), StreamType.valueOf(streamType));

            sessionManager.createDeliverChannel(participant);
        } else {
            log.error("Error: participant {} is not a publisher and role={} and streamType={}", participant.getParticipantPublicId()
                    , participant.getRole(), participant.getStreamType());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to publish video. The user does not have a valid token");
        }


        // deal participant that role changed
        String key;
        if (StreamType.MAJOR.name().equals(streamType)
                && cacheManage.existsConferenceRelativeInfo(key = CacheKeyConstants.getSubscriberSetRollCallKey(session.getSessionId(),
                session.getStartTime(), participant.getUuid()))) {
            sessionManager.setRollCallInSession(sessionManager.getSession(rpcConnection.getSessionId()), participant);

            cacheManage.delConferenceRelativeKey(key);
        }

        // update recording
        if (session.isRecording.get() && participant.ableToUpdateRecord()) {
            sessionManager.updateRecording(session.getSessionId(), participant);
        }
    }
}
