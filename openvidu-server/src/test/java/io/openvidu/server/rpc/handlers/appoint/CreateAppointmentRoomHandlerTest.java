package io.openvidu.server.rpc.handlers.appoint;

import com.alibaba.fastjson.JSON;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.AppointConferenceExample;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.resp.AppointmentRoomResp;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.handlers.appoint.CreateAppointmentRoomHandler;
import io.openvidu.server.utils.DateUtil;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kurento.jsonrpc.internal.client.ClientSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class CreateAppointmentRoomHandlerTest extends TestCase {
    @Autowired
    CreateAppointmentRoomHandler handler;

    @Autowired
    AppointConferenceMapper appointConferenceMapper;

    @Test
    public void test() {


    }


    @Test
    public void conflictTest() {

    }

}