package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.TerminalTypeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * @author geedow
 * @date 2019/11/5 17:00
 */
@Slf4j
@Service
public class SetRollCallHandler extends RpcAbstractHandler {

    public static final Object roll_call_lock = new Object();

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM);
        String type = getStringOptionalParam(request, "type");

        Session conferenceSession = sessionManager.getSession(sessionId);
        Participant targetPart = conferenceSession.getParticipantByUUID(targetId).orElseGet(null);
        Optional<Participant> originatorOp = conferenceSession.getParticipantByUUID(rpcConnection.getUserUuid());

        if (targetPart == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    new JsonObject(), ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            return;
        }
        Participant moderatorPart = conferenceSession.getModeratorPart();
        if (moderatorPart == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    new JsonObject(), ErrorCodeEnum.MODERATOR_NOT_FOUND);
            return;
        }

        if (targetPart.getTerminalType() == TerminalTypeEnum.S) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SIP_CANNOT_BE_A_SPEAKER);
            return;
        }

        // check if target participant is SUBSCRIBER
        if (OpenViduRole.SUBSCRIBER.equals(targetPart.getRole())
                && Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
            ErrorCodeEnum errorCodeEnum;

            // check if the size of major part is over 12
            if (conferenceSession.getPartSize() > conferenceSession.getPresetInfo().getMcuThreshold()) {
                errorCodeEnum = conferenceSession.evictPartInCompositeWhenSubToPublish(targetPart, sessionManager);
            } else {
                // invalid rpc call when size of major part is not up to 12
                log.error("Invalid rpc call when size of major part is:{} in session:{} is not up to {}",
                        conferenceSession.getPartSize(), sessionId,  conferenceSession.getPresetInfo().getMcuThreshold());
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
        if ("applicant".equals(type) && targetPart.getHandStatus() == ParticipantHandStatus.down) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PARTICIPANT_DOWN_HAND_NOW);
            return;
        }
        ErrorCodeEnum errorCode = sessionManager.setRollCallInSession(conferenceSession, targetPart,originatorOp.get());
        //发言
        if (ErrorCodeEnum.SET_ROLL_CALL_SAME_PART.equals(errorCode)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SET_ROLL_CALL_SAME_PART);
            return;
        }
        // update recording
        if (conferenceSession.ableToUpdateRecord()) {
            sessionManager.updateRecording(conferenceSession.getSessionId());
        }
        ((KurentoSession) conferenceSession).asyncUpdateSipComposite();

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

}
