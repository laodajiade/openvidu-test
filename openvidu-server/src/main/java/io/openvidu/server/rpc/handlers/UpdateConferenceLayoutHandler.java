package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.common.enums.LayoutModeTypeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

@Service(ProtocolElements.UPDATE_CONFERENCE_LAYOUT_METHOD)
public class UpdateConferenceLayoutHandler extends ExRpcAbstractHandler<JsonObject> {


    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        String roomId = BindValidate.notEmptyAndGet(params, "roomId");
        LayoutModeTypeEnum layoutModeType = BindValidate.notEmptyAndGet(params, "layoutModeType", LayoutModeTypeEnum.class);
        long timestamp = BindValidate.notEmptyAndGet(params, "timestamp", Long.class);
        JsonArray layout = BindValidate.notEmptyAndGet(params, "layout", JsonArray.class);
        LayoutModeEnum mode = LayoutModeEnum.getLayoutMode(BindValidate.notEmptyAndGet(params, "mode", Integer.class));

        Participant participant = sanityCheckOfSession(rpcConnection);
        Session session = sessionManager.getSession(roomId);

        if (participant.getRole() != OpenViduRole.MODERATOR) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        session.getModeratorLayoutInfo().updateLayout(timestamp, mode, layoutModeType, layout, participant);

        return RespResult.ok();
    }
}
