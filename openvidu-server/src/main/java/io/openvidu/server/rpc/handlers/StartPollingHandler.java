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
 * @date 2020/10/19 10:32
 */
@Slf4j
@Service
public class StartPollingHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, ProtocolElements.START_POLLING_ROOMID_PARAM);
        String time = getStringParam(request,ProtocolElements.START_POLLING_INTERVAL_TIME_PARAM);

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
        SessionPreset preset = session.getPresetInfo();
        if (preset.getPollingStatusInRoom().equals(SessionPresetEnum.on)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.THE_POLLING_HAS_STARTED);
            return;
        }
        preset.setPollingStatusInRoom(SessionPresetEnum.on);

        timerManager.onStartPolling(Integer.parseInt(time), session, this.notificationService);
        //send notify
        this.notificationService.sendNotification(operatePart.getParticipantPrivateId(),
                ProtocolElements.START_POLLING_NOTIFY_METHOD, request.getParams());
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

    }

}
