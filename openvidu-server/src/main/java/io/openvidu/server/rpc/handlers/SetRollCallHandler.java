package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.*;
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

        Session conferenceSession = sessionManager.getSession(sessionId);
        boolean isMcu = Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU);
        // check if target participant is SUBSCRIBER
        if (sessionManager.isSubscriberInSession(sessionId, targetId) && isMcu) {
            ErrorCodeEnum errorCodeEnum;
            Participant subscriberPart = conferenceSession.getParticipantByUserId(targetId);

            // check if the size of major part is up to 12(current)
            if (conferenceSession.getMajorPartSize() == openviduConfig.getMcuMajorPartLimit()) {
                errorCodeEnum = conferenceSession.evictPartInCompositeWhenSubToPublish(subscriberPart, sessionManager);
            } else {
                // invalid rpc call when size of major part is not up to 12
                log.error("Invalid rpc call when size of major part in session:{} is not up to {}",
                        sessionId, openviduConfig.getMcuMajorPartLimit());
                errorCodeEnum = ErrorCodeEnum.INVALID_METHOD_CALL;
            }

            if (Objects.equals(ErrorCodeEnum.SUCCESS, errorCodeEnum)) {
                notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
            } else {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, errorCodeEnum);
            }
            return;
        }

        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        Participant moderatorPart = participants.stream().filter(participant -> Objects.equals(sourceId,
                participant.getUserId()) && Objects.equals(StreamType.MAJOR, participant.getStreamType()) &&
                !Objects.equals(OpenViduRole.THOR, participant.getRole())).findFirst().orElse(null);

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
            assert moderatorPart != null;
            sourceConnectionId = moderatorPart.getParticipantPublicId();
            if (Objects.equals(AccessTypeEnum.web, rpcConnection.getAccessType())) {
                JsonObject firstOrderPart = conferenceSession.getMajorShareMixLinkedArr().get(0).getAsJsonObject();
                if (!firstOrderPart.get("streamType").getAsString().equals(StreamType.SHARING.name())) {
                    sourceConnectionId = firstOrderPart.get("connectionId").getAsString();
                } else {
                    sourceConnectionId = conferenceSession.getMajorShareMixLinkedArr().get(1).getAsJsonObject().get("connectionId").getAsString();
                }

            }
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

        if (isMcu) {
            // change conference layout
            conferenceSession.replacePartOrderInConference(sourceConnectionId, targetConnectionId);
            // json RPC notify KMS layout changed.
            conferenceSession.invokeKmsConferenceLayout();
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.SET_ROLL_CALL_ROOM_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.SET_ROLL_CALL_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.SET_ROLL_CALL_TARGET_ID_PARAM, targetId);
        params.addProperty(ProtocolElements.SET_ROLL_CALL_RAISEHAND_NUMBER_PARAM, raiseHandNum);

        // broadcast the changes of layout
        participants.forEach(participant -> {
            if (!Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                return;
            }
            // SetRollCall notify
            this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.SET_ROLL_CALL_METHOD, params);
            if (isMcu) {
                // broadcast the changes of layout
                this.notificationService.sendNotification(participant.getParticipantPrivateId(),
                        ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, conferenceSession.getLayoutNotifyInfo());
            }
        });

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

    private void sendEndRollCallNotify(Set<Participant> participants, JsonObject params) {
        participants.forEach(participant -> {
            if (!Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                return;
            }
            this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.END_ROLL_CALL_METHOD, params);
        });
    }

}
