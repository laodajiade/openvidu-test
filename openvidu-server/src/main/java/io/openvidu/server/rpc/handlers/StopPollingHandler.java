package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.core.SessionPresetEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author even
 * @date 2020/10/19 15:25
 */
@Slf4j
@Service
public class StopPollingHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, ProtocolElements.STOP_POLLING_ROOMID_PARAM);
        Session session = sessionManager.getSession(roomId);
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        // verify operate permission
        Participant operatePart = session.getPartByPrivateIdAndStreamType(rpcConnection.getParticipantPrivateId(), StreamType.MAJOR);
        if (!operatePart.getRole().isController()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }
        if (session.getPresetInfo().getPollingStatusInRoom().equals(SessionPresetEnum.off)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.POLLING_IS_NOT_STARTED);
            return;
        }
        SessionPreset sessionPreset = session.getPresetInfo();
        sessionPreset.setPollingStatusInRoom(SessionPresetEnum.off);
        timerManager.stopPollingCompensation(roomId);
        //send notify
        session.getParticipants().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                ProtocolElements.STOP_POLLING_NODIFY_METHOD, request.getParams()));
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
