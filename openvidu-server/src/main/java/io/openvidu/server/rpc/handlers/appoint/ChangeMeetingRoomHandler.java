package io.openvidu.server.rpc.handlers.appoint;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointParticipantMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.RoomIdTypeEnums;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.AppointParticipant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import io.openvidu.server.utils.LocalDateTimeUtils;
import io.openvidu.server.utils.RandomRoomIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service(ProtocolElements.CHANGE_MEETING_ROOM_METHOD)
@Slf4j
public class ChangeMeetingRoomHandler extends AbstractAppointmentRoomHandler<JsonObject> {

    @Autowired
    private RandomRoomIdGenerator randomRoomIdGenerator;

    @Autowired
    private AppointParticipantMapper appointParticipantMapper;

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        String ruid = BindValidate.notEmptyAndGet(params, "ruid");
        RoomIdTypeEnums roomIdType = RoomIdTypeEnums.parse(BindValidate.notEmptyAndGet(params, "roomIdType"));
        String roomId;
        if (roomIdType == RoomIdTypeEnums.random) {
            roomId = randomRoomIdGenerator.offerRoomId();
        } else {
            roomId = BindValidate.notEmptyAndGet(params, "roomId");
        }

        AppointConference appointConference = appointConferenceManage.getByRuid(ruid);
        if (appointConference == null) {
            return RespResult.fail(ErrorCodeEnum.APPOINTMENT_CONFERENCE_NOT_EXIST);
        }

        if (!rpcConnection.getUserId().equals(appointConference.getUserId())) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        if (roomIdType == RoomIdTypeEnums.personal && !rpcConnection.getUserUuid().equals(roomId)) {
            log.info("change meeting room to personal room permission limited roomId:{} uuid:{}", roomId, rpcConnection.getUserUuid());
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        if (appointConference.getStatus() != 0) {
            return RespResult.fail(ErrorCodeEnum.APPOINTMENT_STATUS_ERROR);
        }

        AppointmentRoomVO vo = new AppointmentRoomVO();
        vo.setRoomId(roomId);
        List<String> collect = appointParticipantMapper.selectListByRuid(ruid).stream().map(AppointParticipant::getUuid).collect(Collectors.toList());
        vo.setParticipants(collect);
        vo.setEndTime(appointConference.getEndTime().getTime());

        ErrorCodeEnum errorCodeEnum;
        if ((errorCodeEnum = checkService(rpcConnection, vo)) != ErrorCodeEnum.SUCCESS) {
            return RespResult.fail(errorCodeEnum);
        }

        // 若假设原预约会议为A，新切换会议号生成的预约会议为B，切换的会议号，原有下一个预约会议为C，
        // 且距离下一个会议C开始的时间，少于A的原有会议时长，则B的会议时长需对应减少。
        Date endTime = appointConference.getEndTime();
        Optional<AppointConference> nextAppt = appointConferenceManage.getNextAppt(new Date(), roomId);
        if (nextAppt.isPresent()) {
            AppointConference next = nextAppt.get();
            long durationSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), LocalDateTimeUtils.translateFromDate(next.getStartTime()));
            if (durationSeconds <= 300L) {
                return RespResult.fail(ErrorCodeEnum.APPOINT_CONFERENCE_CONFLICT);
            }
            if (endTime.getTime() > DateUtils.addMinutes(next.getStartTime(), -1).getTime()) {
                endTime = DateUtils.addMinutes(next.getStartTime(), -1);
            }
        }

        appointConference.setRoomId(roomId);
        appointConference.setRoomCapacity(vo.getRoomCapacity());
        appointConference.setEndTime(endTime);
        appointConferenceManage.updateById(appointConference);
        return RespResult.ok();
    }
}
