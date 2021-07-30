package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author geedow
 * @date 2019/11/5 17:19
 */
@Slf4j
@Service
public class SubscribeVideoHandler extends RpcAbstractHandler {


    private static final String PUBLISH_ID_PARAM = "publishId";
    private static final String SENDER_UUID_PARAM = "senderUuid";
    private static final String SDP_OFFER_PARAM = "sdpOffer";
    private static final String STREAM_TYPE_PARAM = "streamType";
    private static final String STREAM_MODE_PARAM = "streamMode";


    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        KurentoSession session = (KurentoSession) sessionManager.getSession(rpcConnection.getSessionId());
        if (session == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNRECOGNIZED_API);
            return;
        }

        Participant participant = sanityCheckOfSession(rpcConnection);

        StreamModeEnum streamMode = StreamModeEnum.valueOf(getStringOptionalParam(request, STREAM_MODE_PARAM, StreamModeEnum.SFU_SHARING.name()));
        String publishId = getStringParam(request, PUBLISH_ID_PARAM);
        String sdpOffer = getStringParam(request, SDP_OFFER_PARAM);
        StreamType streamType = StreamType.MAJOR;

        Participant senderPart;
        log.info("11111111111111111 {}",session.getConferenceMode());
        List<String> devParts = Arrays.asList("81103600038-1", "81100212770", "80103600005");
        if (session.getConferenceMode() == ConferenceModeEnum.MCU && devParts.contains(participant.getUuid())) {
            log.info("调试 {}", participant.getUuid());
            publishId = session.getCompositeService().getMixStreamId();
            streamMode = StreamModeEnum.MIX_MAJOR;
        }


        if (streamMode == StreamModeEnum.MIX_MAJOR) {
            if (!Objects.equals(session.getCompositeService().getMixStreamId(), publishId)) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.UNRECOGNIZED_API);
                return;
            }
            senderPart = participant;
        } else {
            String uuid = getStringParam(request, SENDER_UUID_PARAM);
            streamType = StreamType.valueOf(getStringParam(request, STREAM_TYPE_PARAM));
            Optional<Participant> senderPartOp = session.getParticipantByUUID(uuid);
            if (!senderPartOp.isPresent()) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
                return;
            }
            senderPart = senderPartOp.get();
        }

        sessionManager.subscribe(participant, senderPart, streamType, streamMode, sdpOffer, publishId, request.getId());
    }
}
