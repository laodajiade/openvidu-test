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

/**
 * @program: prepaid-platform
 * @description:
 * @author: WuBing
 * @create: 2021-09-14 10:52
 **/
@Service
@Slf4j
public class SetPartSelfDismissMuteHandler extends RpcAbstractHandler {

    @Resource
    protected SessionManager sessionManager;

    @Autowired
    protected RpcNotificationService rpcNotificationService;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        try {
            String sessionId = getStringParam(request, ProtocolElements.SET_PARTSELF_DISMISS_MUTE_ROOMID_PARAM);
            String originator = getStringParam(request, ProtocolElements.SET_PARTSELF_DISMISS_MUTE_ORIGINATOR_PARAM);
            String allowPartDismissMute = getStringParam(request, ProtocolElements.SET_PARTSELF_DISMISS_MUTE_ALLOWPARTDISMISSMUTE_PARAM);
            Session session = sessionManager.getSession(sessionId);
            SessionPreset preset = sessionManager.getPresetInfo(sessionId);
            // verify session valid
            if (Objects.isNull(session)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
                return;
            }
            if (!allowPartDismissMute.equals(SessionPresetEnum.off.name()) && !allowPartDismissMute.equals(SessionPresetEnum.on.name())) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
                return;
            }
            Participant operatePart = session.getParticipantByPrivateId(rpcConnection.getParticipantPrivateId());
            if (!operatePart.getRole().isController()) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.PERMISSION_LIMITED);
                return;
            }
            preset.setAllowPartDismissMute(Objects.equals(allowPartDismissMute, SessionPresetEnum.on.name()) ? SessionPresetEnum.on : SessionPresetEnum.off);
            JsonObject result = new JsonObject();
            result.addProperty("roomId", sessionId);
            result.addProperty("originator", originator);
            result.addProperty("allowPartDismissMute", allowPartDismissMute);
            rpcNotificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.SET_PARTSELF_DISMISS_MUTE_NOTIFY, result);
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        } catch (Exception e) {
            log.error("setMuteAll error {}, {}", request.getParams(), rpcConnection.toString(), e);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_INTERNAL_ERROR);
        }

    }
}
