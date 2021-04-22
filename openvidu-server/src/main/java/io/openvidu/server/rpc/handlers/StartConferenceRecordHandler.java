package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ConferenceRecordStatusEnum;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.LocalDateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class StartConferenceRecordHandler extends RpcAbstractHandler {

    @Value("${min.interval.stop}")
    private Long minIntervalStop;

    @Value("${record.service:true}")
    private boolean recordService = true;

    private static final long lowerLimit = 20 * 1024 * 1024;

    private static final long upperLimit = 100 * 1024 * 1024;


    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, ProtocolElements.START_CONF_RECORD_ROOMID_PARAM);
        boolean forceRec = getBooleanOptionalParam(request, "force");

        Session session;
        if (Objects.isNull(session = sessionManager.getSession(roomId))) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }

        // 校验会议
        ConferenceSearch search = new ConferenceSearch();
        search.setRoomId(roomId);
        search.setStatus(ConferenceStatus.PROCESS.getStatus());
        List<Conference> conferenceList = conferenceMapper.selectBySearchCondition(search);
        if (CollectionUtils.isEmpty(conferenceList)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
            return;
        }

        if (session.getActivePublishers() <= 0) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_STREAMING_ERROR_CODE);
            return;
        }

        Participant participant;
        try {
            participant = sanityCheckOfSession(rpcConnection, "startConferenceRecord");
        } catch (OpenViduException e) {
            return;
        }

        // 权限校验（web：管理员，terminal：主持人）
        if (!participant.getRole().isController()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        // 录制服务是否启用（在有效期内）
        Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
        if (!recordService){
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNSUPPORTED_RECORD_OPERATION);
            return;
        }
        if (corporation.getRecordingExpireDate().getYear() == 1970) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.NO_RECORD_SERVICE);
            return;
        }
        if (LocalDateTimeUtils.toEpochMilli(corporation.getRecordingExpireDate()) < System.currentTimeMillis()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.RECORD_SERVICE_INVALID);
            return;
        }

        // 校验录制存储空间
        // 小于20MB时，拒绝录制，返回13050（record storage exhausted）
        // 小于100MB时，返回13052（record storage less than 100MB）
        List<RoomRecordSummary> roomRecordSummaries = conferenceRecordManage.getAllRoomRecordSummaryByProject(ConferenceRecordSearch.builder()
                .project(rpcConnection.getProject()).build());
        long usedSpaceSize = CollectionUtils.isEmpty(roomRecordSummaries) ? 0L : roomRecordSummaries.stream().mapToLong(RoomRecordSummary::getOccupation).sum();
        long remainStorageSpace = conferenceRecordManage.getCorpRecordStorage(rpcConnection.getProject()).longValue() * 1024 * 1024 - usedSpaceSize;
        if (remainStorageSpace <= lowerLimit) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.RECORD_STORAGE_EXHAUSTED);
            return;
        } else if (remainStorageSpace <= upperLimit && !forceRec) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.RECORD_STORAGE_NOT_ENOUGH);
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
        if (CollectionUtils.isEmpty(existRecordList)) {
            conferenceRecordManage.insertSelective(constructConferenceRecord(rpcConnection, session));
        } else {
            ConferenceRecord existRecord = existRecordList.get(0);
            existRecord.setStatus(ConferenceRecordStatusEnum.WAIT.getStatus());
            existRecord.setRequestStartTime(new Date());
            conferenceRecordManage.updateByPrimaryKey(existRecord);
        }

        // 通知录制服务
        sessionManager.startRecording(roomId);

        //查询录制状态是否在录制中  如果没有通知客户端录制失败重新录制
        ConferenceRecord byRuIdRecordStatus = conferenceRecordManage.getByRuIdRecordStatus(rpcConnection.getUdid());
        try {
            Thread.sleep(3000);
            if (ConferenceRecordStatusEnum.WAIT.getStatus().equals(byRuIdRecordStatus.getStatus())){
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_RECORD_NOT_START);
            }
        }catch (Exception e){
            log.info(e.getMessage());
        }





        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        // 通知与会者开始录制
        notifyStartRecording(rpcConnection.getSessionId());
    }

    private void notifyStartRecording(String sessionId) {
        Session session = sessionManager.getSession(sessionId);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("startRecordingTime", session.getStartRecordingTime());
        session.getParticipants().forEach(participant ->
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.START_CONF_RECORD_METHOD, jsonObject));
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