package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.constants.BrokerChannelConstans;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service(ProtocolElements.URGED_PEOPLE_TO_END_METHOD)
public class UrgedPeopleToEndHandler extends ExRpcAbstractHandler<JsonObject> {


    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        BindValidate.notEmpty(params, "roomId");
        BindValidate.notEmpty(params, "ruid");

        String roomId = getStringParam(request, "roomId");
        String ruid = getStringParam(request, "ruid");

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", ProtocolElements.URGED_PEOPLE_TO_END_METHOD);

        JsonObject param = new JsonObject();
        jsonObject.add("params", param);
        param.addProperty("roomId", roomId);

        cacheManage.publish(BrokerChannelConstans.TO_OPENVIDU_CHANNEL, jsonObject.toString());
        return RespResult.ok();
    }


    public void notifyToModerator(JsonObject params) {
        String roomId = params.get("roomId").getAsString();
        Session session = sessionManager.getSession(roomId);
        if (session == null) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("ruid", session.getRuid());
        json.addProperty("roomId", roomId);
        json.addProperty("timestamp", System.currentTimeMillis());
        notificationService.sendBatchNotification(new HashSet<>(session.getModeratorAndThorPart()),
                ProtocolElements.URGED_PEOPLE_TO_END_NOTIFY_METHOD, json.toString());
    }
}
