package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class GetH5PagesHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String type = getStringOptionalParam(request, ProtocolElements.GETH5PAGES_TYPE_PARAM);
        JsonObject respObj = new JsonObject();
        JsonArray pageArr = new JsonArray(5);
        if (!StringUtils.isEmpty(type) && openviduConfig.getH5PageConfigMap().containsKey(type)) {
            JsonObject pageObj = new JsonObject();
            pageObj.addProperty("type", type);
            pageObj.addProperty("url", openviduConfig.getH5PageConfigMap().get(type));

            pageArr.add(pageObj);
        } else {
            pageArr = gson.fromJson(openviduConfig.getH5PagesInfoConfig(), JsonArray.class);
        }

        respObj.add("pageList", pageArr);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }
}
