package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 17:00
 */
@Slf4j
@Service
public class SetRollCallHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM);

        Session conferenceSession = sessionManager.getSession(sessionId);
        Participant targetPart = conferenceSession.getParticipantByUUID(targetId);
        // check if target participant is SUBSCRIBER
        if (OpenViduRole.SUBSCRIBER.equals(targetPart.getRole())
                && Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
            ErrorCodeEnum errorCodeEnum;

            // check if the size of major part is up to 12(current)
            if (conferenceSession.getMajorPartSize() == openviduConfig.getMcuMajorPartLimit()) {
                errorCodeEnum = conferenceSession.evictPartInCompositeWhenSubToPublish(targetPart, sessionManager);
            } else {
                // invalid rpc call when size of major part is not up to 12
                log.error("Invalid rpc call when size of major part in session:{} is not up to {}",
                        sessionId, openviduConfig.getMcuMajorPartLimit());
                errorCodeEnum = ErrorCodeEnum.SERVER_INTERNAL_ERROR;
            }

            if (Objects.equals(ErrorCodeEnum.SUCCESS, errorCodeEnum)) {
                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

                cacheManage.recordSubscriberSetRollCall(conferenceSession.getSessionId(), conferenceSession.getStartTime(), targetPart.getUuid());
            } else {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, errorCodeEnum);
            }
            return;
        }

        sessionManager.setRollCallInSession(conferenceSession, targetPart);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

}
