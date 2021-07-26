package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.endpoint.SubscriberEndpoint;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Optional<Participant> participantOptional = sessionManager.getParticipantByUUID(rpcConnection.getSessionId(), rpcConnection.getUserUuid());
        if (!participantOptional.isPresent()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNRECOGNIZED_API);
            return;
        }
        Participant participant = participantOptional.get();
        String subscribeId = getStringParam(request, SUBSCRIBE_ID_PARAM);
        SubscriberEndpoint subscriberEndpoint = participant.getSubscribers().get(subscribeId);
        if (subscriberEndpoint == null) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.ENP_POINT_NAME_NOT_EXIST);
            return;
        }

        sessionManager.unsubscribe(participant, subscribeId, request.getId());
    }
}
