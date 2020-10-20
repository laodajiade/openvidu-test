package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author even
 * @date 2020/10/19 16:41
 */
@Slf4j
@Service
public class GetPollingStatusHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, ProtocolElements.GETPOLLINGSTATUS_ROOMID_PARAM);

        Session session = sessionManager.getSession(roomId);
        if (Objects.isNull(session)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        SessionPreset sessionPreset = session.getPresetInfo();
        JsonObject resJson = new JsonObject();
        resJson.addProperty(ProtocolElements.GETPOLLINGSTATUS_STATUS_PARAM, sessionPreset.getPollingStatusInRoom().name());
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), resJson);
    }
}
