package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
            if (Objects.isNull(evictPart)) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
                return;
            }

            sessionManager.evictParticipantByUUID(evictPart.getSessionId(), evictPart.getUuid(), false);

            RpcConnection evictRpcConnection = notificationService.getRpcConnection(evictPart.getParticipantPrivateId());
            if (!Objects.isNull(evictRpcConnection.getSerialNumber())) {
                cacheManage.setDeviceStatus(evictRpcConnection.getSerialNumber(), DeviceStatus.online.name());
            }
        } else {
            log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
            throw new OpenViduException(OpenViduException.Code.USER_UNAUTHORIZED_ERROR_CODE,
                    "Unable to force disconnect. The user does not have a valid token");
        }
    }
}
