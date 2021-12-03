package io.openvidu.server.rpc.handlers.im;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.IMModeEnum;
import io.openvidu.server.core.Notification;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

@Slf4j
@Service(ProtocolElements.SET_IM_MODE_METHOD)
public class SetImModeHandler extends AbstractIMHandler<JsonObject> {

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        String roomId = getStringParam(request, "roomId");
        int operate = getIntParam(request, "operate");

        Session session = sessionManager.getSession(roomId);
        if (session == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }
        IMModeEnum imModeEnum = IMModeEnum.parse(operate);
        session.getPresetInfo().setImMode(imModeEnum.getMode());

        JsonObject notifyParams = new JsonObject();
        notifyParams.addProperty("roomId", roomId);
        notifyParams.addProperty("imMode", imModeEnum.getMode());
        Notification notification = new Notification(ProtocolElements.NOTIFY_SET_IM_MODE_METHOD, notifyParams);
        notification.withParticipantIds(roomId, sessionManager);

        return RespResult.ok(new JsonObject(), notification);
    }
}
