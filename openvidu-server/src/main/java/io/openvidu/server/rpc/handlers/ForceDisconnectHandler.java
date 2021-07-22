package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

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
            participant = sanityCheckOfSession(rpcConnection);
        } catch (OpenViduException e) {
            return;
        }

        if (participant.getRole() == OpenViduRole.MODERATOR) {
            //String connectionId = getStringParam(request, ProtocolElements.FORCEDISCONNECT_CONNECTIONID_PARAM);
            String uuid = getStringParam(request, ProtocolElements.FORCEDISCONNECT_UUID_PARAM);
            Session session = sessionManager.getSession(rpcConnection.getSessionId());
            Optional<Participant> participantOptional = session.getParticipantByUUID(uuid);

            if (!participantOptional.isPresent()) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
                return;
            }

            Participant evictPart = participantOptional.get();

            sessionManager.evictParticipantByUUID(evictPart.getSessionId(), evictPart.getUuid(), Collections.emptyList());

            RpcConnection evictRpcConnection = notificationService.getRpcConnection(evictPart.getParticipantPrivateId());
            if (!Objects.isNull(evictRpcConnection.getSerialNumber())) {
                cacheManage.updateTerminalStatus(evictRpcConnection, TerminalStatus.online);
            }
            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        } else {
            log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to force disconnect. The user does not have a valid token");
        }
    }
}
