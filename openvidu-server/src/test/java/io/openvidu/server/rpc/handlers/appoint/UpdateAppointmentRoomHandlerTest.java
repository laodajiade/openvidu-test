package io.openvidu.server.rpc.handlers.appoint;

import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.AppointConferenceExample;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.resp.AppointmentRoomResp;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;
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
public class UpdateAppointmentRoomHandlerTest extends TestCase {

    @Autowired
    UpdateAppointmentRoomHandler handler;

    @Autowired
    AppointConferenceMapper appointConferenceMapper;


    String roomId = "456456456";

    @Test
    public void test() {

    }


    public AppointmentRoomVO generatorVO() {
        AppointmentRoomVO vo = new AppointmentRoomVO();
        vo.setRuid(null);
        vo.setRoomId(roomId);
        vo.setPassword("");
        vo.setSubject("test subject" + DateUtil.getTimeOfDate(System.currentTimeMillis()));
        vo.setDesc("test desc" + DateUtil.getTimeOfDate(System.currentTimeMillis()));
        vo.setAutoCall(false);
        vo.setStartTime(System.currentTimeMillis() + 16 * 1000 * 60);
        vo.setDuration(60);
        vo.setEndTime(vo.getStartTime() + 60000 * vo.getDuration());

        vo.setParticipants(Arrays.asList("80101900005", "80101900006", "80101900007"));
        return vo;
    }
}