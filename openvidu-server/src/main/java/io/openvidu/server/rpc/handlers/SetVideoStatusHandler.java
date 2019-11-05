package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantMicStatus;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2019/11/5 16:47
 */
@Slf4j
@Service
public class SetVideoStatusHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_VIDEO_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM);
        List<String> targetIds = getStringListParam(request, ProtocolElements.SET_AUDIO_TARGET_IDS_PARAM);
        String status = getStringParam(request, ProtocolElements.SET_AUDIO_STATUS_PARAM);
        if ((Objects.isNull(targetIds) || targetIds.isEmpty() || !Objects.equals(sourceId, targetIds.get(0)))
                && sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        JsonArray tsArray = new JsonArray();
        if (!Objects.isNull(targetIds) && !targetIds.isEmpty()) {
            targetIds.forEach(t -> {
                KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream().filter(s -> Long.valueOf(t)
                        .compareTo(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData")
                                .getAsLong()) == 0).findFirst().get();
                if (part.isStreaming())
                    part.getPublisherMediaOptions().setVideoActive(!status.equals(ParticipantMicStatus.off.name()));
                tsArray.add(t);
            });
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.SET_VIDEO_ROOM_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.SET_VIDEO_SOURCE_ID_PARAM, getStringParam(request, ProtocolElements.SET_VIDEO_SOURCE_ID_PARAM));
        params.add(ProtocolElements.SET_VIDEO_TARGET_IDS_PARAM, tsArray);
        params.addProperty(ProtocolElements.SET_VIDEO_STATUS_PARAM, getStringParam(request, ProtocolElements.SET_VIDEO_STATUS_PARAM));

        sessionManager.getParticipants(sessionId).forEach(participant -> {
            this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                    ProtocolElements.SET_VIDEO_STATUS_METHOD, params);
            if ((Objects.isNull(targetIds) || targetIds.isEmpty()) && !sourceId.equals(gson.fromJson(participant.getClientMetadata(),
                    JsonObject.class).get("clientData").getAsString())) {
                KurentoParticipant part = (KurentoParticipant) participant;
                if (part.isStreaming()) part.getPublisherMediaOptions().setVideoActive(!status.equals(ParticipantMicStatus.off.name()));
            }
        });
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
