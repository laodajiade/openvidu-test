package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import io.netty.util.internal.StringUtil;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.enums.ConferenceRecordStatusEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.ConferenceRecord;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class StopConferenceRecordHandler extends RpcAbstractHandler {

    @Value("${min.interval.start}")
    private Long minIntervalStart;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringOptionalParam(request, ProtocolElements.STOP_CONF_RECORD_ROOMID_PARAM);
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

        // 权限校验（非本人发起不可停止）
        ConferenceRecord record = new ConferenceRecord();
        record.setRuid(session.getRuid());
        List<ConferenceRecord> conferenceRecordList = conferenceRecordManage.getByCondition(record);
        if (CollectionUtils.isEmpty(conferenceRecordList)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_RECORD_NOT_START);
            return;
        }

        ConferenceRecord conferenceRecord = conferenceRecordList.get(0);
        if (conferenceRecord.getStatus().equals(ConferenceRecordStatusEnum.WAIT.getStatus())) {
            if (System.currentTimeMillis() - conferenceRecord.getRequestStartTime().getTime() < 15000) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.CONFERENCE_RECORD_NOT_START);
                return;
            }
            log.error("会议录制开始超时,强行停止,record:{}", JSON.toJSONString(conferenceRecord));
            conferenceRecord.setStatus(ConferenceRecordStatusEnum.FINISH.getStatus());
            conferenceRecordManage.updateByPrimaryKey(conferenceRecord);
        }

        if (!rpcConnection.getUserUuid().equals(conferenceRecord.getRecorderUuid())) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        // 校验会议从结束到开始时间间隔
        Long startRecordingTime;
        if (Objects.nonNull(startRecordingTime = session.getStartRecordingTime())
                && Math.abs(System.currentTimeMillis() - startRecordingTime) < minIntervalStart) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_RECORD_FREQUENT_OPERATION);
            return;
        }

        if (!session.sessionAllowedToStopRecording()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_RECORD_NOT_START);
            return;
        }

        sessionManager.setStopRecordingTime(session.getSessionId(), System.currentTimeMillis());

        // 通知录制服务停止录制视频
        sessionManager.stopRecording(roomId);


        returnAndNotify(rpcConnection, request);
    }

    private void returnAndNotify(RpcConnection rpcConnection, Request<JsonObject> request) {
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
        // 通知与会者停止录制
        notifyStopRecording(rpcConnection.getSessionId());
    }

    private void notifyStopRecording(String sessionId) {
        JsonObject notify = new JsonObject();
        notify.addProperty("reason", CommonConstants.RECORD_STOP_BY_MODERATOR);
        sessionManager.getSession(sessionId).getParticipants().forEach(participant ->
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.STOP_CONF_RECORD_METHOD, notify));
    }
}