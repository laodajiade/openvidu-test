package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantShareStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class SharingControlHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SHARING_CONTROL_ROOMID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.SHARING_CONTROL_TARGETID_PARAM);
        String operation = getStringParam(request, ProtocolElements.SHARING_CONTROL_OPERATION_PARAM);
        getStringParam(request, ProtocolElements.SHARING_CONTROL_SOURCEID_PARAM);
        ParticipantShareStatus shareStatus = ParticipantShareStatus.valueOf(operation);

        if (!OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(sessionId,
                rpcConnection.getParticipantPrivateId()).getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        if (!CollectionUtils.isEmpty(participants)) {
            participants.forEach(p -> {
                if (targetId.equals(p.getUserId().toString())) {
                    p.changeShareStatus(shareStatus);
                }
                if (Objects.equals(StreamType.MAJOR, p.getStreamType()))
                    this.notificationService.sendNotification(p.getParticipantPrivateId(),
                            ProtocolElements.SHARING_CONTROL_NOTIFY, request.getParams());
            });
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
