package io.openvidu.server.controller;

import com.google.gson.Gson;
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

    @PostMapping("test")
    public String test(@RequestParam("id") String id, @RequestParam("method") String method, @RequestBody String params) {

        RpcAbstractHandler rpcHandler = rpcHandlerFactory.getRpcHandler(method);
        String participantPrivateId = id;
        RpcConnection connection = notificationService.getRpcConnections().stream().filter(c -> c.getUserUuid().equals(id)).findFirst().get();
        //RpcConnection connection = notificationService.getRpcConnection(participantPrivateId);

        Request<JsonObject> request = new Request<>();
        request.setId(9999);
        request.setMethod(method);
        request.setParams(new Gson().fromJson(params, JsonObject.class));

        rpcHandler.handRpcRequest(connection, request);
        return "hello world";
    }


}
