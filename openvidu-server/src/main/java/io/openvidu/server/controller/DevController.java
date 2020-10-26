package io.openvidu.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcHandlerFactory;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev")
@Slf4j
public class DevController {


    @Autowired
    RpcHandlerFactory rpcHandlerFactory;

    @Autowired
    RpcNotificationService notificationService;


    private static RespResult result;

    @PostMapping(value = "test",produces = "application/json")
    public @ResponseBody String test(@RequestParam("id") String id, @RequestParam("method") String method, @RequestBody String params) {

        RpcAbstractHandler rpcHandler = rpcHandlerFactory.getRpcHandler(method);
        String participantPrivateId = id;
        RpcConnection connection = notificationService.getRpcConnections().stream().filter(c -> c.getUserUuid().equals(id)).findFirst().get();
        //RpcConnection connection = notificationService.getRpcConnection(participantPrivateId);
        RpcNotificationService notificationService = rpcHandler.getNotificationService();
        result = null;
        try {
            RpcNotificationService r0 = new RpcNotificationService0();
            rpcHandler.setNotificationService(r0);

            Request<JsonObject> request = new Request<>();
            request.setId(9999);
            request.setMethod(method);
            request.setParams(new Gson().fromJson(params, JsonObject.class));

            rpcHandler.handRpcRequest(connection, request);
        } catch (Exception e) {
            e.printStackTrace();
            rpcHandler.setNotificationService(notificationService);
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(result);
    }

    class RpcNotificationService0 extends RpcNotificationService {
        @Override
        public void sendResponse(String participantPrivateId, Integer transactionId, Object result) {
            DevController.result = RespResult.ok(result);
        }

        @Override
        public void sendErrorResponseWithDesc(String participantPrivateId, Integer transactionId, Object data, ErrorCodeEnum errorCodeEnum) {
            DevController.result = RespResult.fail(errorCodeEnum);
        }
    }

}
