package io.openvidu.server.rpc.handlers.appoint;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointParticipantMapper;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.resp.AppointmentRoomResp;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import sun.security.krb5.internal.PAData;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service(ProtocolElements.UPDATE_APPOINTMENT_ROOM_METHOD)
public class UpdateAppointmentRoomHandler extends AbstractAppointmentRoomHandler<AppointmentRoomVO> {


    @Autowired
    private AppointConferenceManage appointConferenceManage;

    @Autowired
    private AppointParticipantMapper appointParticipantMapper;


    @Transactional
    @Override
    public RespResult<AppointmentRoomResp> doProcess(RpcConnection rpcConnection, Request<AppointmentRoomVO> request, AppointmentRoomVO params) {
        BindValidate.notNull(params::getRuid);

        AppointConference appt = appointConferenceManage.getByRuid(params.getRuid());
        if (appt == null) {
            return RespResult.fail(ErrorCodeEnum.APPOINTMENT_CONFERENCE_NOT_EXIST);
        }
        if (appt.getStatus() == ConferenceStatus.FINISHED.getStatus()) {
            return RespResult.fail(ErrorCodeEnum.APPOINTMENT_CONFERENCE_HAS_FINISHED);
        }
        if (appt.getStartTime().before(new Date())) {
            return RespResult.fail(ErrorCodeEnum.THE_CONFERENCE_HAS_STARTED);
        }

        //Conference conference = appointConferenceManage.constructConf(rpcConnection, request);
        // 会议开始时间校验
        if (Objects.isNull(params.getStartTime()) || System.currentTimeMillis() > params.getStartTime()) {
            return RespResult.fail(ErrorCodeEnum.START_TIME_LATE);
        }
        // 会议时长校验
        if (params.getDuration() == null || params.getDuration() <= 0) {
            return RespResult.fail(ErrorCodeEnum.DURATION_TOO_SHORT);
        }
        params.setEndTime(params.getStartTime() + (params.getDuration() * 60000));

        if (CollectionUtils.isEmpty(params.getParticipants())) {
            return RespResult.fail(ErrorCodeEnum.REQUEST_PARAMS_ERROR);
        }

        params.setUserId(appt.getUserId());

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

        appointConferenceManage.updateAppointment(appt, params);

        List<User> users = userManage.queryByUuidList(params.getParticipants());
        appointParticipantMapper.deleteByConferenceRuid(params.getRuid());
        if (Objects.nonNull(users) && !users.isEmpty()) {
            appointParticipantMapper.batchInsert(constructBatchAppoints(params.getRuid(), users));
        }

        // 获取定时任务的id
        List<Long> jobIds = conferenceJobManage.deleteConferenceJobByRuid(params.getRuid());
        // 删除定时任务
        for (Long jobId : jobIds) {
            crowOnceHelper.delCrowOnce(jobId);
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
