package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantVideoStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 16:47
 */
@Slf4j
@Service
public class SetVideoStatusHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_VIDEO_ROOM_ID_PARAM);
        String status = getStringParam(request, ProtocolElements.SET_AUDIO_STATUS_PARAM);
        ParticipantVideoStatus videoStatus = ParticipantVideoStatus.valueOf(status);
        String sourceId = getStringOptionalParam(request, ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM);
        List<String> targetIds = getStringListParam(request, ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM);
        // add params for tourist
        String source = getStringOptionalParam(request, ProtocolElements.SET_VIDEO_SOURCE_PARAM);
        List<String> accountTargets = getStringListParam(request, ProtocolElements.SET_VIDEO_TARGETS_PARAM);

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
                                && !OpenViduRole.NON_PUBLISH_ROLES.contains(s.getRole())).findFirst().orElse(null);
                if (Objects.nonNull(part)) {
                    part.setVideoStatus(videoStatus);
                    tsArray.add(t);
                }
            });
        }

        JsonArray accountArr = new JsonArray();
        if (!Objects.isNull(accountTargets) && !accountTargets.isEmpty()) {
            accountTargets.forEach(account -> {
                KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream()
                        .filter(s -> Objects.equals(account, s.getUuid()) && Objects.equals(StreamType.MAJOR, s.getStreamType())
                                && !OpenViduRole.NON_PUBLISH_ROLES.contains(s.getRole())).findFirst().orElse(null);
                if (Objects.nonNull(part)) {
                    part.setVideoStatus(videoStatus);
                    accountArr.add(account);
                }
            });
        }

        sessionManager.getParticipants(sessionId).forEach(participant -> {
            if (Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                        ProtocolElements.SET_VIDEO_STATUS_METHOD, request.getParams());
            }
        });
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
