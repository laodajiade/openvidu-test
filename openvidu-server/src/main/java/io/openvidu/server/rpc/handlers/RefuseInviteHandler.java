package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.Set;

/**
 * @author geedow
 * @date 2019/11/5 20:05
 */
@Slf4j
@Service
public class RefuseInviteHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.REFUSE_INVITE_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.REFUSE_INVITE_SOURCE_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.REFUSE_INVITE_TARGET_ID_PARAM);
        String reason = getStringParam(request, ProtocolElements.REFUSE_INVITE_REASON_PARAM);

        cancelInviteCompensation(rpcConnection.getUserUuid());

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (!CollectionUtils.isEmpty(participants)) {
            User user = userMapper.selectByUUID(sourceId);
            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.REFUSE_INVITE_ID_PARAM, sessionId);
            params.addProperty(ProtocolElements.REFUSE_INVITE_SOURCE_ID_PARAM, sourceId);
            params.addProperty(ProtocolElements.REFUSE_INVITE_TARGET_ID_PARAM, targetId);
            params.addProperty("username", user.getUsername());
            params.addProperty(ProtocolElements.REFUSE_INVITE_REASON_PARAM, reason);

            for (Participant p : participants) {
                this.notificationService.sendNotification(p.getParticipantPrivateId(),
                        ProtocolElements.REFUSE_INVITE_METHOD, params);
            }
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
