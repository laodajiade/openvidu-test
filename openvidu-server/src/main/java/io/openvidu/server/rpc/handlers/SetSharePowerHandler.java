package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantSharePowerStatus;
import io.openvidu.server.core.Participant;
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
 * @date 2019/11/5 20:14
 */
@Slf4j
@Service
public class SetSharePowerHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_SHARE_POWER_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.SET_SHARE_POWER_SOURCE_ID_PARAM);
        List<String> targetIds = getStringListParam(request, ProtocolElements.SET_SHARE_POWER_TARGET_IDS_PARAM);
        String status = getStringParam(request, ProtocolElements.SET_SHARE_POWER_STATUS_PARAM);

        if ((Objects.isNull(targetIds) || targetIds.isEmpty() || !Objects.equals(sourceId, targetIds.get(0)))
                && sessionManager.getParticipant(sessionId, rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        JsonArray tsArray = new JsonArray();
        if (!Objects.isNull(targetIds) && !targetIds.isEmpty()) {
            targetIds.forEach(t -> tsArray.add(t));
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.SET_SHARE_POWER_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.SET_SHARE_POWER_SOURCE_ID_PARAM, getStringParam(request, ProtocolElements.SET_SHARE_POWER_SOURCE_ID_PARAM));
        params.add(ProtocolElements.SET_SHARE_POWER_TARGET_IDS_PARAM, tsArray);
        params.addProperty(ProtocolElements.SET_SHARE_POWER_STATUS_PARAM, getStringParam(request, ProtocolElements.SET_SHARE_POWER_STATUS_PARAM));
        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (!CollectionUtils.isEmpty(participants)) {
            participants.forEach(p -> {
                long userId = gson.fromJson(p.getClientMetadata(), JsonObject.class).get("clientData").getAsLong();
                if ((Objects.isNull(targetIds) || targetIds.isEmpty()) || targetIds.contains(String.valueOf(userId))) {
                    p.setSharePowerStatus(ParticipantSharePowerStatus.valueOf(status));
                }
                this.notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.SET_SHARE_POWER_METHOD, params);
            });
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
