package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.netty.util.internal.StringUtil;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.RecordingProperties;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.ConferenceRecordStatusEnum;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceRecord;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class StartConferenceRecordHandler extends RpcAbstractHandler {
    @Value("${min.interval.stop}")
    private Long minIntervalStop;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringOptionalParam(request, ProtocolElements.START_CONF_RECORD_ROOMID_PARAM);
        if (StringUtil.isNullOrEmpty(roomId)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        Session session = sessionManager.getSession(roomId);
        if (Objects.isNull(session)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }

        // 校验会议
        ConferenceSearch search = new ConferenceSearch();
        search.setRoomId(roomId);
        search.setStatus(ConferenceStatus.PROCESS.getStatus());
        List<Conference> conferenceList = conferenceMapper.selectBySearchCondition(search);
        if (Objects.isNull(conferenceList) || conferenceList.isEmpty()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }

        if (session.getActivePublishers() <= 0) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_STREAMING_ERROR_CODE);
            return;
        }

        // 权限校验（web：管理员，terminal：主持人）
        boolean permissionLimit;
        if (Objects.equals(AccessTypeEnum.web, rpcConnection.getAccessType())) {
            permissionLimit = userManage.isAdmin(rpcConnection.getUserUuid());
        } else {
            permissionLimit = rpcConnection.getUserUuid().equals(conferenceList.get(0).getRoomId());
        }
        if (!permissionLimit) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        // 校验会议从结束到开始时间间隔
        Long stopRecordingTime;
        if (Objects.nonNull(stopRecordingTime = session.getStopRecordingTime())
                && Math.abs(System.currentTimeMillis() - stopRecordingTime) < minIntervalStop) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_RECORD_FREQUENT_OPERATION);
            return;
        }

        // 判断该会议是否正在录制
        if (!session.sessionAllowedStartToRecord()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_IS_RECORDING);
            return;
        }

        // session中设置开始录制时间
        sessionManager.setStartRecordingTime(roomId, System.currentTimeMillis());

        // 保存记录
        ConferenceRecord condition = new ConferenceRecord();
        condition.setRuid(session.getRuid());
        List<ConferenceRecord> existRecordList = conferenceRecordManage.getByCondition(condition);
        if (Objects.isNull(existRecordList) || existRecordList.isEmpty()) {
            conferenceRecordManage.insertSelective(constructConferenceRecord(rpcConnection, session));
        } else {
            ConferenceRecord existRecord = existRecordList.get(0);
            existRecord.setStatus(ConferenceRecordStatusEnum.WAIT.getStatus());
            existRecord.setRequestStartTime(new Date());
            conferenceRecordManage.updateByPrimaryKey(existRecord);
        }

        // 通知媒体服务开始录制视频
        KurentoSession kurentoSession = (KurentoSession) session;
        recordingManager.startRecording(kurentoSession,
                new RecordingProperties.Builder()
                        .name("")
                        .outputMode(kurentoSession.getSessionProperties().defaultOutputMode())
                        .recordingLayout(kurentoSession.getSessionProperties().defaultRecordingLayout())
                        .customLayout(kurentoSession.getSessionProperties().defaultCustomLayout())
                        .build());

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        // 通知与会者开始录制
        notifyStartRecording(rpcConnection.getSessionId());
    }

    private void notifyStartRecording(String sessionId) {
        sessionManager.getSession(sessionId).getParticipants().forEach(participant ->
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.START_CONF_RECORD_METHOD, new JsonObject()));
    }

    private ConferenceRecord constructConferenceRecord(RpcConnection rpcConnection, Session session) {
        ConferenceRecord conferenceRecord = new ConferenceRecord();
        conferenceRecord.setRuid(session.getRuid());
        conferenceRecord.setRoomId(session.getSessionId());
        conferenceRecord.setRecordCount(0);
        conferenceRecord.setTotalDuration(0);
        conferenceRecord.setStatus(ConferenceRecordStatusEnum.WAIT.getStatus());
        conferenceRecord.setRecorderUuid(rpcConnection.getUserUuid());
        conferenceRecord.setRecorderName("");
        conferenceRecord.setProject(session.getConference().getProject());
        conferenceRecord.setRequestStartTime(new Date());
        return conferenceRecord;
    }

}

