package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
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

/**
 * @author geedow
 * @date 2019/11/5 20:00
 */
@Slf4j
@Service
public class CloseRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Session session;
        ErrorCodeEnum errCode = ErrorCodeEnum.SUCCESS;
        String sessionId = getStringParam(request, ProtocolElements.CLOSE_ROOM_ID_PARAM);
        if (Objects.isNull(session = sessionManager.getSession(sessionId)) || session.isClosed()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED);
            return;
        }

        Participant participant = session.getParticipantByPrivateId(rpcConnection.getParticipantPrivateId());
        if (Objects.nonNull(participant)) {
            if (!participant.getRole().isController()) {
                errCode = ErrorCodeEnum.PERMISSION_LIMITED;
            }
        } else {
            // once participant reconnected, close the room directly without joining room
            // find the participant related to the previous connection and verify the operation permission
            Map partInfo = cacheManage.getPartInfo(rpcConnection.getUserUuid());
            if (!partInfo.isEmpty() && Objects.nonNull(participant = session.getParticipantByUUID(rpcConnection.getUserUuid()))
                    && !participant.getRole().isController()) {
                errCode = ErrorCodeEnum.PERMISSION_LIMITED;
            }

        }
        if (!ErrorCodeEnum.SUCCESS.equals(errCode)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, errCode);
            return;
        }

        closeRoom(rpcConnection, session);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }


    public void closeRoom(RpcConnection rpcConnection, Session session) {
        UseTime.point("closeRoom p1");
        String sessionId = session.getSessionId();
        // set session status: closing
        session.setClosing(true);
        sessionManager.getSession(sessionId).getParticipants().forEach(p -> {
            if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) return;
            notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject());
            RpcConnection rpcConnect = notificationService.getRpcConnection(p.getParticipantPrivateId());
            if (!Objects.isNull(rpcConnect) && !Objects.isNull(rpcConnect.getSerialNumber())) {
                cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
            }
        });
        UseTime.point("closeRoom p2");
        //cancel invite
        cancelAllInviteCompensation(sessionId);
        // TODO: compatible to the delay of leaving room
        this.sessionManager.updateConferenceInfo(sessionId);
        //close room stopPolling
        if (session.getPresetInfo().getPollingStatusInRoom().equals(SessionPresetEnum.on)) {
            SessionPreset sessionPreset = session.getPresetInfo();
            sessionPreset.setPollingStatusInRoom(SessionPresetEnum.off);
            timerManager.stopPollingCompensation(sessionId);
            //send notify
            JsonObject params = new JsonObject();
            params.addProperty("roomId", sessionId);
            notificationService.sendBatchNotification(session.getMajorPartEachIncludeThorConnect(), ProtocolElements.STOP_POLLING_NODIFY_METHOD, params);
        }
        UseTime.Point point = UseTime.getPoint("sessionManager.closeSession.Point");
        this.sessionManager.closeSession(sessionId, EndReason.closeSessionByModerator);
        point.updateTime();
        UseTime.point("closeRoom p5");
        rpcConnection.setReconnected(false);
    }
}
