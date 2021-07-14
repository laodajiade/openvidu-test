package io.openvidu.server.rpc.handlers.appoint;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointParticipantMapper;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.handlers.CloseRoomHandler;
import io.openvidu.server.rpc.handlers.parthistory.DeleteConferenceHistoryHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service(ProtocolElements.CANCEL_APPOINTMENT_ROOM_METHOD)
@Slf4j
public class CancelAppointmentRoomHandler extends AbstractAppointmentRoomHandler<JsonObject> {


    @Autowired
    private AppointConferenceManage appointConferenceManage;

    @Autowired
    private AppointParticipantMapper appointParticipantMapper;

    @Resource
    private ConferenceMapper conferenceMapper;

    @Resource
    private DeleteConferenceHistoryHandler deleteConferenceHistoryHandler;

    @Autowired
    private CloseRoomHandler closeRoomHandler;

    @Transactional
    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject vo) {
        String ruid = getStringParam(request, ProtocolElements.CREATE_ROOM_RUID_PARAM);
        String admin = getStringOptionalParam(request, "admin");

        if (!ruid.startsWith("appt-")) {
            return cancelGeneralConference(rpcConnection, request, vo, ruid);
        }

        AppointConference appointConference = appointConferenceManage.getByRuid(ruid);

        if (appointConference == null) {
            return RespResult.fail(ErrorCodeEnum.APPOINTMENT_CONFERENCE_NOT_EXIST);
        }


        // 权限校验
        if (!StringUtils.equals(rpcConnection.getUserUuid(), appointConference.getModeratorUuid()) && !"admin".equals(admin)) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        if (appointConference.getStatus() == ConferenceStatus.FINISHED.getStatus()) {
            if (StringUtils.startsWith(rpcConnection.getDeviceVersion(), "1.3.2")) {
                return cancelGeneralConference(rpcConnection, request, vo, ruid);
            }
            return RespResult.fail(ErrorCodeEnum.APPOINTMENT_CONFERENCE_HAS_FINISHED);
        }
        cancelApponitment(ruid);

        // 销毁未活跃的房间
        Session sessionNotActive = sessionManager.getSessionNotActive(appointConference.getRoomId());
        log.info("not active session exist:{} ,ruid={}", sessionNotActive != null, sessionNotActive == null ? "" : sessionNotActive.getRuid());
        if (sessionNotActive != null && Objects.equals(sessionNotActive.getRuid(), ruid)) {
            sessionManager.closeSessionAndEmptyCollections(sessionNotActive, EndReason.closeSessionByModerator);
        }

        // 结束会议
        Session session = sessionManager.getSession(appointConference.getRoomId());
        if (session != null && session.getConference().getRuid().equals(ruid)) {
            log.info("close session with cancel appointment conference ,roomId = {} and ruid = {}", appointConference.getRoomId(), appointConference.getRuid());
            sessionManager.closeRoom(rpcConnection, session);
        }

        return RespResult.ok(new JsonObject());
    }

    public void cancelApponitment(String ruid) {
        // 获取定时任务的id
        List<Long> jobIds = conferenceJobManage.deleteConferenceJobByRuid(ruid);
        // 删除定时任务
        for (Long jobId : jobIds) {
            crowOnceHelper.delCrowOnce(jobId);
        }

        appointConferenceManage.deleteByRuid(ruid);
        appointParticipantMapper.deleteByConferenceRuid(ruid);
    }

    /**
     * 取消一般会议
     */
    private RespResult<?> cancelGeneralConference(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject vo, String ruid) {
        Conference conference = conferenceMapper.selectByRuid(ruid);
        if (conference == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }
        if (conference.getStatus() == ConferenceStatus.PROCESS.getStatus()) {
            if (!conference.getModeratorUuid().equals(rpcConnection.getUserUuid())) {
                return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
            }
            // 结束会议
            Session session = sessionManager.getSession(conference.getRoomId());
            if (session != null && session.getConference().getRuid().equals(ruid)) {
                log.info("close session with cancel general conference ,roomId = {} and ruid = {}", conference.getRoomId(), conference.getRuid());
                sessionManager.closeRoom(rpcConnection, session);
                return RespResult.ok(new JsonObject());
            }
        }
        return deleteConferenceHistoryHandler.doProcess(rpcConnection, request, vo);
    }

}
