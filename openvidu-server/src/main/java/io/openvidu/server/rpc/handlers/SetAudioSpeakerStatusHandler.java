package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantSpeakerStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.core.SessionPresetEnum;
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
        String sourceId = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM);
        List<String> accountTargets = getStringListParam(request, ProtocolElements.SET_AUDIO_SPEAKER_TARGET_ID_PARAM);
        String status = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM);

        Session session = sessionManager.getSession(sessionId);
        // verify session valid
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        // verify request parameters
        Participant moderator = session.getPartByPrivateIdAndStreamType(rpcConnection.getParticipantPrivateId(), StreamType.MAJOR);
        if (CollectionUtils.isEmpty(accountTargets) && !moderator.getRole().isController()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        if (!moderator.getRole().isController()) {
            SessionPreset preset = session.getPresetInfo();
            if (preset.getAllowPartOperSpeaker().equals(SessionPresetEnum.off)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.MODERATOR_PROHIBIT_ON_SPEAKER);
                return;
            }
        }

        if (CollectionUtils.isEmpty(accountTargets)) {
            session.getParticipants().forEach(participant -> {
                if (Objects.equals(StreamType.MAJOR, participant.getStreamType()) && !OpenViduRole.THOR.equals(participant.getRole())) {
                    participant.changeSpeakerStatus(ParticipantSpeakerStatus.valueOf(status));
                }
            });
        } else {
            session.getParticipants().forEach(participant -> {
                if (Objects.equals(StreamType.MAJOR, participant.getStreamType()) && !OpenViduRole.THOR.equals(participant.getRole())
                        && accountTargets.contains(participant.getUuid())) {
                    participant.changeSpeakerStatus(ParticipantSpeakerStatus.valueOf(status));
                }
            });
        }

        JsonObject notifyObj = request.getParams().deepCopy();
        notifyObj.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_USERNAME_PARAM,session.getParticipantByUUID(sourceId).getUsername());
        Set<Participant> participants = session.getParticipants();
        if (!CollectionUtils.isEmpty(participants)) {
            for (Participant p: participants) {
                if (Objects.equals(StreamType.MAJOR, p.getStreamType())) {
                    this.notificationService.sendNotification(p.getParticipantPrivateId(),
                            ProtocolElements.SET_AUDIO_SPEAKER_STATUS_METHOD, notifyObj);
                }
            }
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
