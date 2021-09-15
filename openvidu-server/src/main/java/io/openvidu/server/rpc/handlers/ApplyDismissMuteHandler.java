package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.*;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.Optional;

/**
 * @program:
 * @description:
 * @author: WuBing
 * @create: 2021-09-13 17:49
 **/
@Slf4j
@Service
public class ApplyDismissMuteHandler extends RpcAbstractHandler {


    @Resource
    protected SessionManager sessionManager;

    @Autowired
    protected RpcNotificationService rpcNotificationService;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        try {
            String sessionId = getStringParam(request, ProtocolElements.APPLY_DISMISS_MUTE_ROOMID_PARAM);
            String originator = getStringParam(request, ProtocolElements.APPLY_DISMISS_MUTE_ORIGINATOR_PARAM);
            Session session = sessionManager.getSession(sessionId);
            SessionPreset preset = sessionManager.getPresetInfo(sessionId);
            // verify session valid
            if (Objects.isNull(session)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
                return;
            }
            Optional<Participant> originatorPart = session.getParticipantByUUID(originator);
            if (!originatorPart.isPresent()) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject(), ErrorCodeEnum.PARTICIPANT_NOT_FOUND);
                return;
            }
            //墙下人员不允许使用此接口
            if (originatorPart.get().getOrder() > preset.getSfuPublisherThreshold() - 1) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject(), ErrorCodeEnum.INVALID_METHOD_CALL);
                return;
            }
            // 允许自主解除静音模式 不允许使用此接口
            if (preset.getAllowPartDismissMute().equals(SessionPresetEnum.on)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject(), ErrorCodeEnum.INVALID_METHOD_CALL);
                return;
            }
            JsonObject result = new JsonObject();
            result.addProperty("roomId", sessionId);
            result.addProperty("originator", originator);
            rpcNotificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.APPLY_DISMISS_MUTE_NOTIFY, result);
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        } catch (Exception e) {
            log.error("setMuteAll error {}, {}", request.getParams(), rpcConnection.toString(), e);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
        }
    }
}
