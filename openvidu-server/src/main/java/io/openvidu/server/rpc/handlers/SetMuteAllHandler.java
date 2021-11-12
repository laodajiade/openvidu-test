package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.core.SessionPresetEnum;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @program:
 * @description:
 * @author: WuBing
 * @create: 2021-09-13 14:10
 **/

@Slf4j
@Service
public class SetMuteAllHandler extends RpcAbstractHandler {

    @Autowired
    SessionManager sessionManager;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        try {
            String sessionId = getStringParam(request, ProtocolElements.SET_MUTE_ALL_ROOMID_PARAM);
            String originator = getStringParam(request, ProtocolElements.SET_MUTE_ALL_ORIGINATOR_PARAM);
            String quietStatusInRoom = getStringParam(request, ProtocolElements.SET_MUTE_ALL_QUIETSTATUSINROOM_PARAM);
            Session session = sessionManager.getSession(sessionId);
            // verify session valid
            if (Objects.isNull(session)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
                return;
            }

            if (!quietStatusInRoom.equals(SessionPresetEnum.off.name()) && !quietStatusInRoom.equals(SessionPresetEnum.smart.name())) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
                return;
            }
            // verify operate permission
            Participant operatePart = session.getParticipantByPrivateId(rpcConnection.getParticipantPrivateId());
            if (!operatePart.getRole().isController()) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.PERMISSION_LIMITED);
                return;
            }
            sessionManager.setMuteAll(sessionId, originator, quietStatusInRoom.equals(SessionPresetEnum.off.name()) ? SessionPresetEnum.off : SessionPresetEnum.smart);
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        } catch (Exception e) {
            log.error("setMuteAll error {}, {}", request.getParams(), rpcConnection.toString(), e);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
        }


    }
}
