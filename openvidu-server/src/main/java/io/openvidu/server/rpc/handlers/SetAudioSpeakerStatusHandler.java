package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantSpeakerStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author geedow
 * @date 2019/11/5 20:11
 */
@Slf4j
@Service
public class SetAudioSpeakerStatusHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM);
        String source = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM);
        List<String> accountTargets = getStringListParam(request, ProtocolElements.SET_AUDIO_TARGETS_PARAM);
        String status = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM);

        Session session = sessionManager.getSession(sessionId);
        // verify session valid
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        // verify request parameters
        Participant moderator = session.getModeratorPart();
        if (CollectionUtils.isEmpty(accountTargets) && !source.equals(moderator.getUuid())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        if (CollectionUtils.isEmpty(accountTargets)) {
            session.getParticipants().forEach(participant -> {
                if (Objects.equals(StreamType.MAJOR, participant.getStreamType()) && !OpenViduRole.THOR.equals(participant.getRole())) {
                    participant.setSpeakerStatus(ParticipantSpeakerStatus.valueOf(status));
                }
            });
        } else {
            session.getParticipants().forEach(participant -> {
                if (Objects.equals(StreamType.MAJOR, participant.getStreamType()) && !OpenViduRole.THOR.equals(participant.getRole())
                        && accountTargets.contains(participant.getUuid())) {
                    participant.setSpeakerStatus(ParticipantSpeakerStatus.valueOf(status));
                }
            });
        }


        Set<Participant> participants = session.getParticipants();
        if (!CollectionUtils.isEmpty(participants)) {
            for (Participant p: participants) {
                if (Objects.equals(StreamType.MAJOR, p.getStreamType())) {
                    this.notificationService.sendNotification(p.getParticipantPrivateId(),
                            ProtocolElements.SET_AUDIO_SPEAKER_STATUS_METHOD, request.getParams());
                }
            }
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
