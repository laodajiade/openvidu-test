package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.*;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * @author geedow
 * @date 2019/11/5 20:00
 */
@Slf4j
@Service
public class CloseRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.CLOSE_ROOM_ID_PARAM);

//        if (!Objects.isNull(session = sessionManager.getSessionNotActive(sessionId))) {
//            closeRoomNotActive(rpcConnection, session);
//            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
//            return;
//        }
        Session session;
        if (Objects.isNull(session = sessionManager.getSession(sessionId)) || session.isClosed()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED);
            return;
        }
        if (!session.getConference().getModeratorUuid().equals(rpcConnection.getUserUuid()) && rpcConnection.getAccessType() != AccessTypeEnum.web) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

//        Optional<Participant> participantOptional = session.getParticipantByUUID(rpcConnection.getUserUuid());
//
//        if (!participantOptional.isPresent()) {
//            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
//                    null, ErrorCodeEnum.PERMISSION_LIMITED);
//            return;
//        }
//        Participant participant = participantOptional.get();
//        if (participant.getRole() != OpenViduRole.MODERATOR) {
//            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
//                    null, ErrorCodeEnum.PERMISSION_LIMITED);
//            return;
//        }

        sessionManager.closeRoom(rpcConnection, session);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
