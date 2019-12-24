package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
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

        int raiseHandNum = 0;
        String sourceConnectionId = null;
        String targetConnectionId = null;
        Set<Participant> participants = sessionManager.getParticipants(sessionId);
        for (Participant participant : participants) {
            if (Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                if (targetId.equals(participant.getUserId())) {
                    participant.setHandStatus(ParticipantHandStatus.endSpeaker);
                    targetConnectionId = participant.getParticipantPublicId();
                }

                if (sourceId.equals(participant.getUserId()) && !Objects.equals(OpenViduRole.THOR, participant.getRole())) {
                    sourceConnectionId = participant.getParticipantPublicId();
                }

                if (Objects.equals(participant.getHandStatus(), ParticipantHandStatus.up)) {
                    raiseHandNum++;
                }
            }
        }

        Session conferenceSession = sessionManager.getSession(sessionId);
        if (Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
            // change conference layout
            conferenceSession.replacePartOrderInConference(sourceConnectionId, targetConnectionId);
            // json RPC notify KMS layout changed.
            conferenceSession.invokeKmsConferenceLayout();
        }

        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.END_ROLL_CALL_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, targetId);
        params.addProperty(ProtocolElements.END_ROLL_CALL_RAISEHAND_NUMBER_PARAM, raiseHandNum);

        // broadcast the changes of layout
        participants.forEach(participant -> {
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
