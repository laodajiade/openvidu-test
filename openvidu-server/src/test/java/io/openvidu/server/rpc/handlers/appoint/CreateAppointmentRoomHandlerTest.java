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
        String roomId = "123456789";
        // clear conference
        AppointConferenceExample example = new AppointConferenceExample();
        example.createCriteria().andRoomIdEqualTo(roomId);
        appointConferenceMapper.deleteByExample(example);

        ClientSession session = new ClientSession("123456", null);
        RpcConnection rpcConnection = new RpcConnection(session);

        rpcConnection.setUserId(267L);
        rpcConnection.setProject("alibb");
        rpcConnection.setUserUuid("uuid");

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

        System.out.println(JSON.toJSONString(vo));
        RespResult<AppointmentRoomResp> result = handler.doProcess(rpcConnection, null, vo);


        Assert.assertEquals(result.getCode(), ErrorCodeEnum.SUCCESS);
        Assert.assertEquals(result.getResult().getRoomId(), vo.getRoomId());
        Assert.assertNotNull(result.getResult().getRuid());


        System.out.println("result ruid = " + result.getResult().getRuid());

    }


    @Test
    public void conflictTest() {
        String roomId = "99999999";

        // clear conference
        AppointConferenceExample example = new AppointConferenceExample();
        example.createCriteria().andRoomIdEqualTo(roomId);
        appointConferenceMapper.deleteByExample(example);


        ClientSession session = new ClientSession("123456", null);
        RpcConnection rpcConnection = new RpcConnection(session);

        rpcConnection.setUserId(267L);
        rpcConnection.setProject("alibb");
        rpcConnection.setUserUuid("uuid");

        AppointmentRoomVO vo = new AppointmentRoomVO();

        vo.setRuid(null);
        vo.setRoomId(roomId);
        vo.setPassword("");
        vo.setSubject("test subject" + DateUtil.getTimeOfDate(System.currentTimeMillis()));
        vo.setDesc("test desc" + DateUtil.getTimeOfDate(System.currentTimeMillis()));
        vo.setAutoCall(false);
        vo.setStartTime(System.currentTimeMillis() + 15000);
        vo.setDuration(60);
        vo.setEndTime(vo.getStartTime() + 60000 * vo.getDuration());

        vo.setParticipants(Arrays.asList("80101900005", "80101900006", "80101900007"));


        RespResult<AppointmentRoomResp> result = handler.doProcess(rpcConnection, null, vo);


        Assert.assertEquals(result.getCode(), ErrorCodeEnum.SUCCESS);
        Assert.assertEquals(result.getResult().getRoomId(), vo.getRoomId());
        Assert.assertNotNull(result.getResult().getRuid());

        System.out.println("result ruid = " + result.getResult().getRuid());


        // conflict
        vo.setRuid(null);
        result = handler.doProcess(rpcConnection, null, vo);
        Assert.assertEquals(result.getCode(), ErrorCodeEnum.APPOINT_CONFERENCE_CONFLICT);

    }

}