package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantSpeakerStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
        String targetId = getStringOptionalParam(request, ProtocolElements.SET_AUDIO_SPEAKER_TARGET_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM);
        String status = getStringParam(request, ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM);


        if (!Objects.equals(sourceId, targetId)
                && sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        if (!StringUtils.isEmpty(targetId)) {
            KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream()
                    .filter(s -> Objects.equals(targetId, s.getUserId()) && Objects.equals(StreamType.MAJOR, s.getStreamType())
                            && !Objects.equals(OpenViduRole.THOR, s.getRole())).findFirst().get();
            part.setSpeakerStatus(ParticipantSpeakerStatus.valueOf(status));
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_TARGET_ID_PARAM, targetId);
        params.addProperty(ProtocolElements.SET_AUDIO_SPEAKER_STATUS_PARAM, status);
        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (!CollectionUtils.isEmpty(participants)) {
            for (Participant p: participants) {
                this.notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.SET_AUDIO_SPEAKER_STATUS_METHOD, params);
            }
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
