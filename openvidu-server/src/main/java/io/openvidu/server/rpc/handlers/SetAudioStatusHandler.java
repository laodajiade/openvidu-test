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
        String status = getStringParam(request, ProtocolElements.SET_AUDIO_STATUS_PARAM);
        ParticipantMicStatus micStatus = ParticipantMicStatus.valueOf(status);
        String sourceId = getStringOptionalParam(request, ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM);
        // add params for tourist
        String source = getStringOptionalParam(request, ProtocolElements.SET_AUDIO_SOURCE_PARAM);
        List<String> accountTargets = getStringListParam(request, ProtocolElements.SET_AUDIO_TARGETS_PARAM);

        // SUBSCRIBER part role can not operate audio status
        Participant sourcePart;
        Session session = sessionManager.getSession(sessionId);
        if (!StringUtils.isEmpty(source)) {
            sourcePart = session.getParticipantByUUID(source);
        } else {
            sourcePart = session.getParticipantByUserId(Long.valueOf(sourceId));
        }
        if (Objects.isNull(sourcePart)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.INVALID_METHOD_CALL);
            return;
        }

        JsonArray accountArr = new JsonArray();
        if (!Objects.isNull(accountTargets) && !accountTargets.isEmpty()) {
            if (ParticipantMicStatus.on.equals(micStatus)) {
                accountTargets.forEach(account -> {
                    KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream()
                            .filter(s -> Objects.equals(account, s.getUuid()) && Objects.equals(StreamType.MAJOR, s.getStreamType())
                                    && !OpenViduRole.ONLY_SHARE.equals(s.getRole())
                                    && !OpenViduRole.NON_PUBLISH_ROLES.contains(s.getRole())).findFirst().orElse(null);
                    if (Objects.nonNull(part)) {
                        part.changeMicStatus(micStatus);
                        accountArr.add(account);
                    }
                });
            } else {
                accountTargets.forEach(account -> {
                    KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream()
                            .filter(s -> Objects.equals(account, s.getUuid())
                                    && Objects.equals(StreamType.MAJOR, s.getStreamType())).findFirst().orElse(null);
                    if (Objects.nonNull(part)) {
                        part.changeMicStatus(micStatus);
                        accountArr.add(account);
                    } else {
                        log.warn("uuid:{} set audio status fail ,it not exist", account);
                    }
                });
            }

        } else {
            Set<Participant> participants = session.getMajorPartExcludeModeratorConnect();
            if (!CollectionUtils.isEmpty(participants)) {
                participants.forEach(participant -> {
                    if (ParticipantMicStatus.on.equals(micStatus) && !OpenViduRole.NON_PUBLISH_ROLES.contains(participant.getRole())) {
                        participant.changeMicStatus(micStatus);
                    }
                    if (ParticipantMicStatus.off.equals(micStatus)) {
                        participant.changeMicStatus(micStatus);
                    }
                });
            }
        }

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (!CollectionUtils.isEmpty(participants)) {
            for (Participant p: participants) {
                if (Objects.equals(StreamType.MAJOR, p.getStreamType())) {
                    this.notificationService.sendNotification(p.getParticipantPrivateId(),
                            ProtocolElements.SET_AUDIO_STATUS_METHOD, request.getParams());
                }
            }
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
