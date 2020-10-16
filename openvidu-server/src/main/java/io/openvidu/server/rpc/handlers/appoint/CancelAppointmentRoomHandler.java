package io.openvidu.server.rpc.handlers.appoint;

import com.alibaba.druid.util.StringUtils;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointParticipantMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service(ProtocolElements.CANCEL_APPOINTMENT_ROOM_METHOD)
public class CancelAppointmentRoomHandler extends AbstractAppointmentRoomHandler<JsonObject> {


    @Autowired
    private AppointConferenceManage appointConferenceManage;

    @Autowired
    private AppointParticipantMapper appointParticipantMapper;

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject vo) {
        String ruid = getStringParam(request, ProtocolElements.CREATE_ROOM_RUID_PARAM);
        String admin = getStringOptionalParam(request, "admin");
        AppointConference appointConference = appointConferenceManage.getByRuid(ruid);

        if (appointConference == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }


        // 权限校验
        if (!StringUtils.equals(rpcConnection.getUserUuid(), appointConference.getModeratorUuid()) && !"admin".equals(admin)) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        // 删除会议
        conferenceMapper.deleteByRuid(ruid);


        // 获取定时任务的id
        List<Long> jobIds = conferenceJobManage.deleteConferenceJobByRuid(ruid);
        // 删除定时任务
        for (Long jobId : jobIds) {
            crowOnceHelper.delCrowOnce(jobId);
        }

        appointConferenceManage.deleteByRuid(ruid);
        appointParticipantMapper.deleteByConferenceRuid(ruid);

        // todo 取消会议后要关闭会议
       /*
        Session session = sessionManager.getSessionNotActiveWithRuid(ruid);
        if (!Objects.isNull(session)) {
            sessionManager.cleanAppointmentCollections(session.getSessionId());
        }
        */

        return RespResult.ok(new JsonObject());
    }
}
