package io.openvidu.server.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.MetriceUtils;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcHandlerFactory;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.rpc.handlers.im.SendMsgHandler;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RestController
@RequestMapping("/dev")
@Slf4j
public class DevController {


    @Autowired
    RpcHandlerFactory rpcHandlerFactory;

    @Autowired
    RpcNotificationService notificationService;


    private static RespResult result;

    @GetMapping("report")
    public String report() {
        return "<pre>"+ MetriceUtils.report()+"</pre>";
    }

    @GetMapping("reportReset")
    public String reportReset() {
        MetriceUtils.reset(5);
        return "reset success";
    }

    @GetMapping("sensitiveWord")
    public String sensitiveWord(@RequestParam("word") String word) {
        SendMsgHandler.words = word.split(",");
        return "ok";
    }

    @GetMapping("downloadReport")
    public void downloadReport(HttpServletResponse response) throws Exception {
        response.setContentType("application//vnd.ms-excel;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("report" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt", StandardCharsets.UTF_8.name());
        response.setHeader("Content-disposition", "attachment;filename=" + fileName);
        response.getOutputStream().println(MetriceUtils.report());
    }

    @PostMapping(value = "test",produces = "application/json")
    public @ResponseBody String test(@RequestParam("id") String id, @RequestParam("method") String method, @RequestBody String params) {

        RpcAbstractHandler rpcHandler = rpcHandlerFactory.getRpcHandler(method);
        String participantPrivateId = id;
        Optional<RpcConnection> connectionOptional = notificationService.getRpcConnections().stream().filter(c -> c.getUserUuid().equals(id)).findFirst();
        if (!connectionOptional.isPresent()) {
            return new GsonBuilder().setPrettyPrinting().create().toJson(RespResult.fail(ErrorCodeEnum.USER_NOT_EXIST));
        }
        RpcConnection connection = connectionOptional.get();
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
