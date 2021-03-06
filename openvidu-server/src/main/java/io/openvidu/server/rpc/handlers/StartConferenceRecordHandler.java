package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.enums.ConferenceRecordStatusEnum;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.DeployTypeEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPresetEnum;
import io.openvidu.server.job.DognelScheduledExecutor;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.LocalDateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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


        Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());

        // ????????????
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

        Participant participant = sanityCheckOfSession(rpcConnection);

        // ???????????????web???????????????terminal???????????????
        if (!participant.getRole().isController()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        Role role;
        User user = userMapper.selectByUUID(participant.getUuid());

        if (user == null) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_EXIST);
            return;
        }


        if (user.getType().equals(0)) {
            if (Objects.isNull(role = roleMapper.selectUserOperationPermission(participant.getUuid()))
                    || !role.getPrivilege().contains("recording_conference_room_allowed")) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.NOT_RECORDING_PERMISSION);
                return;
            }
        }

        if (session.getPresetInfo().getAllowRecord() == SessionPresetEnum.off) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.UNSUPPORTED_RECORD_OPERATION);
            return;
        }

        if (DognelScheduledExecutor.dongleInfo != null) {
            DongleInfo dongleInfo = DognelScheduledExecutor.dongleInfo;
            if (dongleInfo.getRecordingLicense() > 0) {
                recordService = true;
            } else {
                recordService = false;
            }

            int recordingNum = conferenceRecordManage.getRecordNumByProject(corporation.getProject());
            if (recordingNum > dongleInfo.getRecordingLicense()) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.RECORDING_SERVER_UPPER_LIMIT);
                return;
            }
        }

        // ?????????????????????????????????????????????
        if (!recordService) {
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

        // ????????????????????????
        // ??????20MB???????????????????????????13050???record storage exhausted???
        // ??????100MB????????????13052???record storage less than 100MB???
        if (envConfig.deployType == DeployTypeEnum.SASS) {
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
        }

        // ??????????????????????????????????????????
        Long stopRecordingTime;
        if (Objects.nonNull(stopRecordingTime = session.getStopRecordingTime())
                && Math.abs(System.currentTimeMillis() - stopRecordingTime) < minIntervalStop) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_RECORD_FREQUENT_OPERATION);
            return;
        }

        // ?????????????????????????????????
        if (!session.sessionAllowedStartToRecord()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_IS_RECORDING);
            return;
        }

        // session???????????????????????????
        sessionManager.setStartRecordingTime(roomId, System.currentTimeMillis());

        // ????????????
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

        // ??????????????????
        sessionManager.startRecording(roomId);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        // ???????????????????????????
        notifyStartRecording(rpcConnection.getSessionId());

        asyncCheckRecordStatus(roomId, session, participant);
    }

    private void asyncCheckRecordStatus(String roomId, Session session, Participant participant) {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                //????????????????????????????????????  ????????????==0  ???????????????????????????????????????
                ConferenceRecord recordStatus = conferenceRecordManage.getByRuIdRecordStatus(session.getRuid());
                if (ConferenceRecordStatusEnum.WAIT.getStatus().equals(recordStatus.getStatus())) {
                    log.warn("?????????????????????????????? {},{}", session.getSessionId(), session.getRuid());
                    // ????????????????????????????????????
                    sessionManager.stopRecording(roomId);
                    JsonObject notify = new JsonObject();
                    // notify.addProperty("reason", "serverInternalError");
                    notify.addProperty("reason", CommonConstants.SERVER_INTERNAL_ERROR);
                    notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.STOP_CONF_RECORD_METHOD, notify);
                }
            } catch (Exception e) {
                log.info("asyncCheckRecordStatus error ", e);
            }
        }).start();
    }

    private void notifyStartRecording(String sessionId) {
        Session session = sessionManager.getSession(sessionId);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("startRecordingTime", session.getStartRecordingTime());
        this.notificationService.sendBatchNotificationConcurrent(session.getParticipants(), ProtocolElements.START_CONF_RECORD_METHOD, jsonObject);
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