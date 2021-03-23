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

    }
}