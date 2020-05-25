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
public class GetConferenceRecordStatusHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringOptionalParam(request, ProtocolElements.GET_CONF_RECORD_ROOMID_PARAM);
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

        JsonObject respJson = new JsonObject();
        boolean conferenceRecordStatus = session.getConferenceRecordStatus();
        respJson.addProperty("conferenceRecordStatus", conferenceRecordStatus);
        if (conferenceRecordStatus) {
            respJson.addProperty("startRecordingTime", session.getStartRecordingTime());
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}

