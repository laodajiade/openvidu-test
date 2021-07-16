package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ShareCastPlayStrategy;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author chosongi
 * @date 2019/12/26 20:29
 */

@Slf4j
@Service
public class UploadShareCastPlayStrategyHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String shareCastPlayStrategy = getStringParam(request, ProtocolElements.UPLOADSHARECASTPLAYSTRATEGY_STRATEGY_PARAM);
        getStringParam(request, ProtocolElements.UPLOADSHARECASTPLAYSTRATEGY_CONNECTIONID_PARAM);
        ShareCastPlayStrategy.valueOf(shareCastPlayStrategy);

        // notify the strategy change to the web THOR
        List<Participant> moderatorRoleParts = sessionManager.getParticipants(rpcConnection.getSessionId()).stream()
                .filter(participant -> OpenViduRole.MODERATOR_ROLES.contains(participant.getRole())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(moderatorRoleParts)) {
            moderatorRoleParts.forEach(part -> this.notificationService.sendNotification(part.getParticipantPrivateId(),
                    ProtocolElements.UPLOADSHARECASTPLAYSTRATEGY_NOTIFY, request.getParams()));
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
