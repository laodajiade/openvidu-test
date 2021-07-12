package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class UnpublishVideoHandler extends RpcAbstractHandler {

    public static final String PUBLISH_ID_PARAMS = "publishId";

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Optional<Participant> participantOptional = sessionManager.getParticipantByUUID(rpcConnection.getSessionId(), rpcConnection.getUserUuid());
        if (!participantOptional.isPresent()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNRECOGNIZED_API);
            return;
        }
        Participant participant = participantOptional.get();

        String publishId = getStringParam(request, PUBLISH_ID_PARAMS);

        if (enableToOperate(participant, publishId)) {
            if (!sessionManager.unpublishStream(sessionManager.getSession(rpcConnection.getSessionId()), publishId,
                    participant, request.getId(), EndReason.unpublish)) {
                return;
            }

            // broadcast the changes of layout
            Session conferenceSession = sessionManager.getSession(rpcConnection.getSessionId());
            if (Objects.equals(conferenceSession.getConferenceMode(), ConferenceModeEnum.MCU)) {
                conferenceSession.getParticipants().forEach(part -> {
                    if (!Objects.equals(StreamType.MAJOR, part.getStreamType())) return;
                    // broadcast the changes of layout
                    this.notificationService.sendNotification(part.getParticipantPrivateId(),
                            ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, conferenceSession.getLayoutNotifyInfo());
                });
            }
        } else {
            log.error("Error: participant {} is neither a moderator nor a thor.", participant.getParticipantPublicId());
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_STREAMING_ERROR_CODE);
        }
    }

    private boolean enableToOperate(Participant participant, String streamId) {
        KurentoParticipant kParticipant = (KurentoParticipant) participant;
        return kParticipant.getPublishers().values().stream().anyMatch(ep -> ep.getStreamId().equals(streamId));
    }
}
