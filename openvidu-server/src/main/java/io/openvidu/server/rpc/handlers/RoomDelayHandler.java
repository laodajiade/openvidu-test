package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 20:33
 */
@Slf4j
@Service
public class RoomDelayHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.ROOM_DELAY_ID_PARAM);

        if (sessionManager.getSession(sessionId).getConfDelayTime() > openviduConfig.getVoipDelayMaxTime() * 60 * 60) {
            log.warn("conference:{} delay too long time:{} hour.", sessionId, openviduConfig.getVoipDelayMaxTime());
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_TOO_LONG);
            return ;
        }

        sessionManager.getSession(sessionId).incDelayConfCnt();
        sessionManager.getSession(sessionId).getParticipants().forEach(p -> {
            if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) return;
            notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.ROOM_DELAY_METHOD, new JsonObject());
        });
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
