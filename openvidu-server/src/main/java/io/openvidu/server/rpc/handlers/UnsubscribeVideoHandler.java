package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamModeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.endpoint.SubscriberEndpoint;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * @author geedow
 * @date 2019/11/5 17:42
 */
@Slf4j
@Service
public class UnsubscribeVideoHandler extends RpcAbstractHandler {

    public static final String SUBSCRIBE_ID_PARAM = "subscribeId";
    private static final String STREAM_MODE_PARAM = "streamMode";
    private static final String SENDER_UUID_PARAM = "senderUuid";
    private static final String STREAM_TYPE_PARAM = "streamType";

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Optional<Participant> participantOptional = sessionManager.getParticipantByUUID(rpcConnection.getSessionId(), rpcConnection.getUserUuid());
        StreamModeEnum streamMode = StreamModeEnum.valueOf(getStringOptionalParam(request, STREAM_MODE_PARAM, StreamModeEnum.SFU_SHARING.name()));

        if (!participantOptional.isPresent()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNRECOGNIZED_API);
            return;
        }
        Participant participant = participantOptional.get();

        String subscribeId = getStringOptionalParam(request, SUBSCRIBE_ID_PARAM);
        if (streamMode == StreamModeEnum.MIX_MAJOR) {
            subscribeId = participant.getMixSubscriber() == null ? "" : participant.getMixSubscriber().getStreamId();
        } else if (StringUtils.isBlank(subscribeId)) {
            String sender = getStringParam(request, SENDER_UUID_PARAM);
            StreamType streamType = StreamType.valueOf(getStringParam(request, STREAM_TYPE_PARAM));
            String trait = sender + "_" + streamType.name();
            subscribeId = participant.getSubscribers().keySet().stream().filter(id -> id.contains(trait)).findFirst().orElse(null);
        }

        SubscriberEndpoint subscriberEndpoint = participant.getSubscribers().get(subscribeId);
        if (subscriberEndpoint == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.ENP_POINT_NAME_NOT_EXIST);
            return;
        }

        sessionManager.unsubscribe(participant, subscribeId, request.getId());
    }
}
