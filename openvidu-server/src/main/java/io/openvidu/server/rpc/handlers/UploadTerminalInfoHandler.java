package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author chosongi
 * @date 2020/4/22 21:56
 */
@Slf4j
@Service
public class UploadTerminalInfoHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String ability;
        String functionality;
        rpcConnection.setAbility(ability = getStringOptionalParam(request, ProtocolElements.UPLOADTERMINALINFO_ABILITY_PARAM));
        rpcConnection.setFunctionality(functionality = getStringOptionalParam(request, ProtocolElements.UPLOADTERMINALINFO_FUNCTIONALITY_PARAM));

        Session session;
        if (!StringUtils.isEmpty(rpcConnection.getSessionId()) &&
                Objects.nonNull(session = sessionManager.getSession(rpcConnection.getSessionId()))) {
            ConcurrentMap<String, Participant> samePrivateIdParts = session.getSamePrivateIdParts(rpcConnection.getParticipantPrivateId());
            if (Objects.nonNull(samePrivateIdParts) && !samePrivateIdParts.isEmpty()) {
                samePrivateIdParts.forEach((streamType, participant) -> {
                    participant.setAbility(ability);
                    participant.setFunctionality(functionality);
                });

                // notify
                JsonObject params = request.getParams();
                params.addProperty("uuid", rpcConnection.getUserUuid());
                Set<Participant> participants = session.getParticipants();
                notificationService.sendBatchNotification(participants, ProtocolElements.TERMINAL_INOF_MODIFIED_NOTIFY_METHOD, params);
            } else {
                log.error("NOT FIND THE PARTICIPANT WHEN UPLOAD TERMINAL INFO.");
            }
        } else {
            log.warn("User:{} did not join a conference session yet.", rpcConnection.getUserUuid());
        }

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
