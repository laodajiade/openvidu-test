package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
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
import org.springframework.util.StringUtils;

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
        Optional<Participant> participantOptional = session.getParticipantByUUID(rpcConnection.getUserUuid());
        if (!participantOptional.isPresent()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNRECOGNIZED_API);
            return;
        }

        Participant participant = participantOptional.get();


        String streamModeStr;
        StreamModeEnum streamMode = !StringUtils.isEmpty(streamModeStr = getStringOptionalParam(request, STREAM_MODE_PARAM))
                ? StreamModeEnum.valueOf(streamModeStr) : null;

        String publishId = getStringParam(request, PUBLISH_ID_PARAM);
        String uuid = getStringParam(request, SENDER_UUID_PARAM);
        StreamType streamType = StreamType.valueOf(getStringParam(request, STREAM_TYPE_PARAM));
//        publishId = publishId.substring(0, Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) ?
//                publishId.lastIndexOf("_") : publishId.indexOf("_"));
        String sdpOffer = getStringParam(request, SDP_OFFER_PARAM);

        Optional<Participant> senderPartOp = session.getParticipantByUUID(uuid);
        if (!senderPartOp.isPresent()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
            return;
        }

        sessionManager.subscribe(participant, senderPartOp.get(), streamType, streamMode, sdpOffer, publishId, request.getId());
    }
}
