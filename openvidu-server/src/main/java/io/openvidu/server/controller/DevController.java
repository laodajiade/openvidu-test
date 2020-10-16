package io.openvidu.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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


    private static Object result;

    @PostMapping("test")
    public Object test(@RequestParam("id") String id, @RequestParam("method") String method, @RequestBody String params) {

        RpcAbstractHandler rpcHandler = rpcHandlerFactory.getRpcHandler(method);
        String participantPrivateId = id;
        RpcConnection connection = notificationService.getRpcConnections().stream().filter(c -> c.getUserUuid().equals(id)).findFirst().get();
        //RpcConnection connection = notificationService.getRpcConnection(participantPrivateId);
        RpcNotificationService notificationService = rpcHandler.getNotificationService();

        RpcNotificationService r0 = new RpcNotificationService0();
        rpcHandler.setNotificationService(r0);

        Request<JsonObject> request = new Request<>();
        request.setId(9999);
        request.setMethod(method);
        request.setParams(new Gson().fromJson(params, JsonObject.class));

        rpcHandler.handRpcRequest(connection, request);

        rpcHandler.setNotificationService(notificationService);

        if ("com.google.gson.JsonObject".equals(result.getClass().getName())) {
            result = new GsonBuilder().setPrettyPrinting().create().toJson(result);
        }

        return result;
    }

    class RpcNotificationService0 extends RpcNotificationService {
        public void sendResponse(String participantPrivateId, Integer transactionId, Object result) {
            DevController.result = result;
        }
    }

}
