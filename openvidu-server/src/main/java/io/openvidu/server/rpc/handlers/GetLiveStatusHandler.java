package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.netty.util.internal.StringUtil;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class GetLiveStatusHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringOptionalParam(request, ProtocolElements.GET_LIVE_STATUS_ROOMID_PARAM);
        if (StringUtil.isNullOrEmpty(roomId)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        Session session = sessionManager.getSession(roomId);
        if (Objects.isNull(session)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }

        if (!rpcConnection.getUserUuid().equals(roomId)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        JsonObject respJson = new JsonObject();
        boolean livingStatus = session.getLivingStatus();
        respJson.addProperty("livingStatus", livingStatus);
        if (livingStatus) {
            respJson.addProperty("livingUrl", session.getLivingUrl());
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}

