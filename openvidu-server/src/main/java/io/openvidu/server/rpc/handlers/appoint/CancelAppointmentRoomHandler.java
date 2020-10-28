package io.openvidu.server.rpc.handlers.appoint;

import com.alibaba.druid.util.StringUtils;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointParticipantMapper;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service(ProtocolElements.CANCEL_APPOINTMENT_ROOM_METHOD)
public class CancelAppointmentRoomHandler extends AbstractAppointmentRoomHandler<JsonObject> {


    @Autowired
    private AppointConferenceManage appointConferenceManage;

    @Autowired
    private AppointParticipantMapper appointParticipantMapper;

    @Resource
    private ConferenceMapper conferenceMapper;

    @Resource
    ConferencePartHistoryMapper conferencePartHistoryMapper;

    @Transactional
    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject vo) {
        String ruid = getStringParam(request, ProtocolElements.CREATE_ROOM_RUID_PARAM);
        String admin = getStringOptionalParam(request, "admin");

        if (!ruid.startsWith("appt-")) {
            return delGeneral(rpcConnection, ruid, admin);
        }

        AppointConference appointConference = appointConferenceManage.getByRuid(ruid);

        if (appointConference == null) {
            return RespResult.fail(ErrorCodeEnum.APPOINTMENT_CONFERENCE_NOT_EXIST);
        }


        // 权限校验
        if (!StringUtils.equals(rpcConnection.getUserUuid(), appointConference.getModeratorUuid()) && !"admin".equals(admin)) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        // 删除会议
        conferenceMapper.deleteByRuid(ruid);
        conferencePartHistoryMapper.deleteByRuid(ruid);

        // 获取定时任务的id
        List<Long> jobIds = conferenceJobManage.deleteConferenceJobByRuid(ruid);
        // 删除定时任务
        for (Long jobId : jobIds) {
            crowOnceHelper.delCrowOnce(jobId);
        }

        appointConferenceManage.deleteByRuid(ruid);
        appointParticipantMapper.deleteByConferenceRuid(ruid);

        // 销毁未活跃的房间
        Session sessionNotActive = sessionManager.getSessionNotActive(appointConference.getRoomId());
        if (sessionNotActive != null && sessionNotActive.getRuid().equals(ruid)) {
            sessionManager.closeSessionAndEmptyCollections(sessionNotActive, EndReason.closeSessionByModerator);
        }

        return RespResult.ok(new JsonObject());
    }

    public RespResult<?> delGeneral(RpcConnection rpcConnection, String ruid, String admin) {
        Conference conference = conferenceMapper.selectByRuid(ruid);

        if (conference == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }

        // 权限校验
        if (!Objects.equals(rpcConnection.getUserId(), conference.getUserId()) && !"admin".equals(admin)) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        conferenceMapper.deleteByRuid(ruid);
        conferencePartHistoryMapper.deleteByRuid(ruid);
        return RespResult.ok(new JsonObject());
    }
}
