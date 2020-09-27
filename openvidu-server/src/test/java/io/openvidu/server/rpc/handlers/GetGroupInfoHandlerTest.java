package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.RpcConnection;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.jsonrpc.internal.client.ClientSession;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class GetGroupInfoHandlerTest extends TestCase {

    @Autowired
    GetGroupInfoHandler handler;

    @Test
    public void test() {
        ClientSession session = new ClientSession("123456", null);
        RpcConnection rpcConnection = new RpcConnection(session);

        Request<JsonObject> request = new Request<>();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("groupId", "14");
        jsonObject.addProperty("pageNum", "1");
        jsonObject.addProperty("pageSize", "1");
        request.setParams(jsonObject);


        RespResult<?> pageResultRespResult = handler.doProcess(rpcConnection, request, jsonObject);
        System.out.println(pageResultRespResult.getResult().toString());
    }
}