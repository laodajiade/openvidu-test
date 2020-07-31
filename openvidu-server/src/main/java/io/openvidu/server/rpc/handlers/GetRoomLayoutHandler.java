package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/8 10:57
 */
@Slf4j
@Service
public class GetRoomLayoutHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.GETROOMLAYOUT_ROOM_ID_PARAM);
        if (Objects.isNull(sessionId)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }
        io.openvidu.server.core.Session conferenceSession = this.sessionManager.getSession(sessionId);
        if (Objects.isNull(conferenceSession)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }
        LayoutModeEnum layoutModeEnum = conferenceSession.getLayoutMode();
        JsonArray layoutInfo = conferenceSession.getLayoutInfo();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(ProtocolElements.GETROOMLAYOUT_MODE_PARAM, layoutModeEnum.getMode());
        jsonObject.addProperty(ProtocolElements.GETROOMLAYOUT_TYPE_PARAM, conferenceSession.getLayoutChangeTypeEnum().name());
        jsonObject.add(ProtocolElements.GETROOMLAYOUT_LAYOUT_PARAM, layoutInfo);
        if (conferenceSession.getModeratorIndex() >= 0) {
            jsonObject.addProperty(ProtocolElements.GETROOMLAYOUT_MODERATOR_INDEX_PARAM_PARAM, conferenceSession.getModeratorIndex());
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }
}
