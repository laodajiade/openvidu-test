package io.openvidu.server.rpc.handlers;

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

import java.util.Set;

@Slf4j
@Service
public class StopPublishSharingHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.STOP_SHARE_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.STOP_PUBLISH_SHARING_SOURCEID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.STOP_PUBLISH_SHARING_TARGETID_PARAM);

        if (!OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(sessionId,
                rpcConnection.getParticipantPrivateId()).getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        JsonObject reqParams = request.getParams();
        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (!CollectionUtils.isEmpty(participants)) {
            participants.forEach(p -> {
                if (targetId.equals(p.getUserId())) {
                    p.setSharePowerStatus(ParticipantSharePowerStatus.off);
                }
                this.notificationService.sendNotification(p.getParticipantPrivateId(),
                        ProtocolElements.STOP_PUBLISH_SHARING_NOTIFY, reqParams);
            });
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
