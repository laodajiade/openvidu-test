package io.openvidu.server.rpc.handlers.appoint;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointParticipantMapper;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.resp.AppointmentRoomResp;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service(ProtocolElements.CREATE_APPOINTMENT_ROOM_METHOD)
public class CreateAppointmentRoomHandler extends AbstractAppointmentRoomHandler<AppointmentRoomVO> {

    @Autowired
    private AppointConferenceManage appointConferenceManage;

    @Autowired
    private AppointParticipantMapper appointParticipantMapper;

    @Resource
    private CorporationMapper corporationMapper;


    @Override
    public RespResult<AppointmentRoomResp> doProcess(RpcConnection rpcConnection, Request<AppointmentRoomVO> request, AppointmentRoomVO params) {

        BindValidate.notEmpty(params::getParticipants);

        //Conference conference = appointConferenceManage.constructConf(rpcConnection, request);

        // 会议开始时间校验
        if (params.getStartTime() != 0 && System.currentTimeMillis() > params.getStartTime()) {
            return RespResult.fail(ErrorCodeEnum.START_TIME_LATE);
        } else if (params.getStartTime() == 0) {
            params.setStartTime(System.currentTimeMillis() + 15000);
        }

        // 会议时长校验
        if (params.getDuration() == null || params.getDuration() <= 0) {
            return RespResult.fail(ErrorCodeEnum.DURATION_TOO_SHORT);
        }

        params.setEndTime(params.getStartTime() + (params.getDuration() * 60000));

        // 检验容量
        Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
        params.setRoomCapacity(corporation.getCapacity());
        if (corporation.getCapacity() < params.getParticipants().size()) {
            return RespResult.fail(ErrorCodeEnum.ROOM_CAPACITY_LIMITED);
        }

        // 校验有效期
        if (corporation.getExpireDate().getTime() + ONE_DAY_MILLIS < params.getEndTime()) {
            return RespResult.fail(ErrorCodeEnum.APPOINTMENT_TIME_AFTER_SERVICE_EXPIRED);
        }
        // 判断是否会议冲突
        if (appointConferenceManage.isConflict(params)) {
            return RespResult.fail(ErrorCodeEnum.APPOINT_CONFERENCE_CONFLICT);
        }
        params.setUserId(rpcConnection.getUserId());
        // 保存预约会议
        appointConferenceManage.insert(params, rpcConnection);
        List<User> users = userManage.queryByUuidList(params.getParticipants());

        if (Objects.nonNull(users) && !users.isEmpty()) {
            appointParticipantMapper.batchInsert(constructBatchAppoints(params.getRuid(), users));
        }

        // 创建定时任务
        JsonObject respJson = new JsonObject();
        respJson.addProperty("ruid", params.getRuid());
        createTimer(params, new HashSet<>(params.getParticipants()), respJson);

        AppointmentRoomResp resp = new AppointmentRoomResp();
        resp.setRuid(params.getRuid());
        resp.setRoomId(params.getRoomId());
        return RespResult.ok(resp);
    }


}
