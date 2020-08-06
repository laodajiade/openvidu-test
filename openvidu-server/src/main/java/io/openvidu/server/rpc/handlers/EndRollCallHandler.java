package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/**
 * @author geedow
 * @date 2019/11/5 17:03
 */
@Service
public class EndRollCallHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.END_ROLL_CALL_SOURCE_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM);

        Participant sourcePart = null, targetPart = null;
        Session conferenceSession = sessionManager.getSession(sessionId);
        Set<Participant> participants = conferenceSession.getParticipants();
        for (Participant participant : participants) {
            if (Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                if (targetId.equals(participant.getUuid())) {
                    participant.setHandStatus(ParticipantHandStatus.endSpeaker);
                    targetPart = participant;
                }

                if (Objects.equals(sourceId, participant.getUuid()) && !Objects.equals(OpenViduRole.THOR, participant.getRole())) {
                    sourcePart = participant;
                }
            }
        }

        if (Objects.isNull(sourcePart) || Objects.isNull(targetPart)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(),
                    request.getId(), null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        if (Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
            // change conference layout
            conferenceSession.replacePartOrderInConference(sourcePart.getParticipantPublicId(), targetPart.getParticipantPublicId());
            // json RPC notify KMS layout changed.
            conferenceSession.invokeKmsConferenceLayout();
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.END_ROLL_CALL_SOURCE_ID_PARAM, sourcePart.getUuid());
        params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, targetPart.getUuid());

        // broadcast the changes of layout
        participants.forEach(participant -> {
            if (!Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                return;
            }
            this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.END_ROLL_CALL_METHOD, params);
            if (Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
                // broadcast the changes of layout
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY,
                        conferenceSession.getLayoutNotifyInfo());
            }
        });

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
