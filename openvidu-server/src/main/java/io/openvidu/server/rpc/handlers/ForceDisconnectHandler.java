package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.EndReason;
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
 * @date 2019/11/5 19:33
 */
@Slf4j
@Service
public class ForceDisconnectHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "forceDisconnect");
        } catch (OpenViduException e) {
            return;
        }

        if (sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant)) {
            String connectionId = getStringParam(request, ProtocolElements.FORCEDISCONNECT_CONNECTIONID_PARAM);
            Session session = sessionManager.getSession(rpcConnection.getSessionId());
            Participant evictPart = session.getParticipantByPublicId(connectionId);
            String moderatePublicId = null;
            String speakerId = null;
            Set<Participant> participants = sessionManager.getParticipants(rpcConnection.getSessionId());
            if (Objects.equals(ParticipantHandStatus.speaker, evictPart.getHandStatus())) {
                JsonObject params = new JsonObject();
                params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, rpcConnection.getSessionId());
                params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, evictPart.getUserId());

                for (Participant participant1 : participants) {
                    if (Objects.equals(StreamType.MAJOR, participant1.getStreamType())) {
                        if (participant1.getRole().equals(OpenViduRole.MODERATOR))
                            moderatePublicId = participant1.getParticipantPublicId();
                        if (Objects.equals(ParticipantHandStatus.speaker, participant1.getHandStatus()))
                            speakerId = participant1.getParticipantPublicId();

                        this.notificationService.sendNotification(participant1.getParticipantPrivateId(),
                                ProtocolElements.END_ROLL_CALL_METHOD, params);
                    }
                }

                evictPart.setHandStatus(ParticipantHandStatus.endSpeaker);
            }

            Participant sharePart = session.getPartByPrivateIdAndStreamType(evictPart.getParticipantPrivateId(), StreamType.SHARING);
            if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU)) {
                session.leaveRoomSetLayout(evictPart, !Objects.equals(speakerId, evictPart.getParticipantPublicId()) ?
                        speakerId : moderatePublicId);
                // json RPC notify KMS layout changed.
                session.invokeKmsConferenceLayout();

                if (Objects.nonNull(sharePart)) {
                    session.leaveRoomSetLayout(sharePart, !Objects.equals(speakerId, sharePart.getParticipantPublicId()) ?
                            speakerId : moderatePublicId);
                    // json RPC notify KMS layout changed.
                    session.invokeKmsConferenceLayout();
                }

                for (Participant participant1 : participants) {
                    if (Objects.equals(StreamType.MAJOR, participant1.getStreamType())) {
                        notificationService.sendNotification(participant1.getParticipantPrivateId(),
                                ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, session.getLayoutNotifyInfo());
                    }
                }
            }


            sessionManager.evictParticipant(evictPart, participant, request.getId(), EndReason.forceDisconnectByUser);
            if (Objects.nonNull(sharePart)) {
                sessionManager.evictParticipant(sharePart, null, null, EndReason.forceDisconnectByUser);
            }

            RpcConnection evictRpcConnection = notificationService.getRpcConnection(evictPart.getParticipantPrivateId());
            if (!Objects.isNull(evictRpcConnection.getSerialNumber()) && !Objects.equals(StreamType.SHARING, evictPart.getStreamType())) {
                cacheManage.setDeviceStatus(evictRpcConnection.getSerialNumber(), DeviceStatus.online.name());
            }

            session.putPartOnWallAutomatically(sessionManager);

        } else {
            log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to force disconnect. The user does not have a valid token");
        }
    }
}
