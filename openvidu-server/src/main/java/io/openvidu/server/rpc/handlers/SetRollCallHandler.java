package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

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
        String sourceId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_SOURCE_ID_PARAM);
        String targetId = getStringParam(request, ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM);

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        Participant moderatorPart = participants.stream().filter(participant -> Objects.equals(sourceId,
                participant.getUserId()) && Objects.equals(StreamType.MAJOR, participant.getStreamType())).findAny().orElse(null);
        /*boolean permitted = !Objects.isNull(moderatorPart) && OpenViduRole.MODERATOR_ROLES.contains(moderatorPart.getRole());
        if (!permitted) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }*/

        int raiseHandNum = 0;
        String sourceConnectionId;
        String targetConnectionId;
        Participant existSpeakerPart = null;
        Participant targetPart = null;
        for (Participant participant : participants) {
            if (Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())) {
                    existSpeakerPart = participant;
                }
                if (participant.getUserId().equals(targetId)) {
                    targetPart = participant;
                }
                if (Objects.equals(participant.getHandStatus(), ParticipantHandStatus.up)) {
                    raiseHandNum++;
                }
            }
        }

        assert targetPart != null;
        targetPart.setHandStatus(ParticipantHandStatus.speaker);
        targetConnectionId = targetPart.getParticipantPublicId();
        if (Objects.isNull(existSpeakerPart)) {
            // switch layout with moderator
            sourceConnectionId = moderatorPart.getParticipantPublicId();
        } else {
            // switch layout with current speaker participant
            sourceConnectionId = existSpeakerPart.getParticipantPublicId();
            // change current speaker part status and send notify
            existSpeakerPart.setHandStatus(ParticipantHandStatus.endSpeaker);
            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, sessionId);
            params.addProperty(ProtocolElements.END_ROLL_CALL_SOURCE_ID_PARAM, sourceId);
            params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, existSpeakerPart.getUserId());
            params.addProperty(ProtocolElements.END_ROLL_CALL_RAISEHAND_NUMBER_PARAM, raiseHandNum);
            sendEndRollCallNotify(participants, params);
        }

        // change conference layout
        Session conferenceSession = sessionManager.getSession(sessionId);
        conferenceSession.replacePartOrderInConference(sourceConnectionId, targetConnectionId);
        // json RPC notify KMS layout changed.
        conferenceSession.invokeKmsConferenceLayout();

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.SET_ROLL_CALL_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, targetId);
        params.addProperty(ProtocolElements.SET_ROLL_CALL_RAISEHAND_NUMBER_PARAM, raiseHandNum);

        // broadcast the changes of layout
        JsonObject notifyResult = new JsonObject();
        notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY_MODE_PARAM, conferenceSession.getLayoutMode().getMode());
        notifyResult.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM, conferenceSession.getCurrentPartInMcuLayout());
        notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_AUTOMATICALLY_PARAM, conferenceSession.isAutomatically());

        participants.forEach(participant -> {
            // SetRollCall notify
            this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_ROLL_CALL_METHOD, params);

            // broadcast the changes of layout
            this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, notifyResult);
        });

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

    private void sendEndRollCallNotify(Set<Participant> participants, JsonObject params) {
        participants.forEach(participant -> this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                ProtocolElements.END_ROLL_CALL_METHOD, params));
    }

}
