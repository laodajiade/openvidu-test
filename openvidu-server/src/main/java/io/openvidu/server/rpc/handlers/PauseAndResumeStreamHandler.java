package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.OperationMode;
import io.openvidu.server.core.Participant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author even
 * @date 2020/9/15 11:55
 */
@Slf4j
@Service
public class PauseAndResumeStreamHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        OperationMode operationMode = OperationMode.valueOf(getStringParam(request, ProtocolElements.PAUSEANDRESUMESTREAM_OPERATION_PARAM));
        List<JsonObject> streams = getJsonObjectListParam(request,ProtocolElements.PAUSEANDRESUMESTREAM_STREAMS_PARAM);
        log.info("request param streams:{}", streams);
        if (CollectionUtils.isEmpty(streams)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }
        for (JsonObject json : streams) {
            String mediaType = json.get("mediaType").getAsString();
            String connectionId = json.get("connectionId").getAsString();
            Participant pausePart = sessionManager.getParticipant(rpcConnection.getSessionId(),rpcConnection.getParticipantPrivateId());
            Participant targetPart = sessionManager.getSession(rpcConnection.getSessionId()).getParticipantByPublicId(connectionId);
            if (Objects.isNull(targetPart)) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
                return;
            }
            try {
                sessionManager.pauseAndResumeStream(pausePart,targetPart,operationMode,mediaType);
            } catch (Exception e) {
                log.error("request method:{} error:{}",request.getMethod(),e);
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
                return;
            }

            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        }
    }
}