package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantMicStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author geedow
 * @date 2019/11/5 16:43
 */
@Slf4j
@Service
public class SetAudioStatusHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_AUDIO_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM);
        String status = getStringParam(request, ProtocolElements.SET_AUDIO_STATUS_PARAM);
        List<String> targetIds = getStringListParam(request, ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM);
        // add params for tourist
        String source = getStringParam(request, ProtocolElements.SET_AUDIO_SOURCE_PARAM);
        List<String> accountTargets = getStringListParam(request, ProtocolElements.SET_AUDIO_TARGETS_PARAM);

        if ((Objects.isNull(targetIds) || targetIds.isEmpty() || !Objects.equals(sourceId, targetIds.get(0))) &&
                !OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(sessionId,
                        rpcConnection.getParticipantPrivateId(), StreamType.MAJOR).getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        // SUBSCRIBER part role can not operate audio status
        Participant sourcePart;
        Session session = sessionManager.getSession(sessionId);
        if (!StringUtils.isEmpty(source)) {
            sourcePart = session.getParticipantByUUID(source);
        } else {
            sourcePart = session.getParticipantByUserId(sourceId);
        }
        if (Objects.isNull(sourcePart) || OpenViduRole.SUBSCRIBER.equals(sourcePart.getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.INVALID_METHOD_CALL);
            return;
        }

        JsonArray tsArray = new JsonArray();
        if (!Objects.isNull(targetIds) && !targetIds.isEmpty()) {
            targetIds.forEach(t -> {
                KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream()
                        .filter(s -> Objects.equals(t, s.getUserId()) && Objects.equals(StreamType.MAJOR, s.getStreamType())
                                && !OpenViduRole.NON_PUBLISH_ROLES.contains(s.getRole())).findFirst().get();
                if (part.isStreaming())
                    part.getPublisherMediaOptions().setAudioActive(!status.equals(ParticipantMicStatus.off.name()));

                tsArray.add(t);
            });
        }

        JsonArray accountArr = new JsonArray();
        if (!Objects.isNull(accountTargets) && !accountTargets.isEmpty()) {
            accountTargets.forEach(account -> {
                KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream()
                        .filter(s -> Objects.equals(account, s.getUuid()) && Objects.equals(StreamType.MAJOR, s.getStreamType())
                                && !OpenViduRole.NON_PUBLISH_ROLES.contains(s.getRole())).findFirst().get();
                if (part.isStreaming()) {
                    part.getPublisherMediaOptions().setAudioActive(!status.equals(ParticipantMicStatus.off.name()));
                }

                accountArr.add(account);
            });
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM, sourceId);
        params.add(ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM, tsArray);

        if (!StringUtils.isEmpty(source)) {
            params.addProperty(ProtocolElements.SET_AUDIO_SOURCE_PARAM, source);
            params.add(ProtocolElements.SET_AUDIO_TARGETS_PARAM, accountArr);
        }
        params.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, getStringParam(request, ProtocolElements.SET_AUDIO_STATUS_PARAM));
        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (!CollectionUtils.isEmpty(participants)) {
            for (Participant p: participants) {
                if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) continue;
                this.notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.SET_AUDIO_STATUS_METHOD, params);

                if ((Objects.isNull(targetIds) || targetIds.isEmpty()) &&
                        !sourceId.equals(gson.fromJson(p.getClientMetadata(), JsonObject.class).get("clientData").getAsString())) {
                    KurentoParticipant part = (KurentoParticipant) p;
                    if (part.isStreaming()) part.getPublisherMediaOptions().setAudioActive(!status.equals(ParticipantMicStatus.off.name()));
                }
            }
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
