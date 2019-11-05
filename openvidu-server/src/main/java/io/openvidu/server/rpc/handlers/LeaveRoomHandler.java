package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author chosongi
 * @date 2019/11/5 17:15
 */
@Slf4j
@Service
public class LeaveRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.LEAVEROOM_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.LEAVEROOM_SOURCE_ID_PARAM);
        String streamType = getStringParam(request, ProtocolElements.LEAVEROOM_STREAM_TYPE_PARAM);
        if (StringUtils.isEmpty(sessionId) || StringUtils.isEmpty(sourceId) || StringUtils.isEmpty(streamType)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, StreamType.valueOf(streamType));
        } catch (OpenViduException e) {
            if (updateReconnectInfo(rpcConnection)) {
                log.info("close previous participant info.");
                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
            }
            return;
        }

        sessionManager.leaveRoom(participant, request.getId(), EndReason.disconnect, false);
        log.info("Participant {} has left session {}", participant.getParticipantPublicId(),
                rpcConnection.getSessionId());
    }
}
