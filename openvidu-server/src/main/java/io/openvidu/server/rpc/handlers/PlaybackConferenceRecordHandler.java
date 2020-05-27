package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.netty.util.internal.StringUtil;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.ConferenceRecord;
import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class PlaybackConferenceRecordHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Long id = getLongParam(request, ProtocolElements.PLAYBACK_CONF_RECORD_ID_PARAM);

        ConferenceRecordInfo conferenceRecordInfo = conferenceRecordInfoManage.selectByPrimaryKey(id);
        if (Objects.isNull(conferenceRecordInfo)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_RECORD_NOT_EXIST);
            return;
        }
        // 权限校验（非本人会议室不可播放）
        ConferenceRecord record = new ConferenceRecord();
        record.setRuid(conferenceRecordInfo.getRuid());
        List<ConferenceRecord> conferenceRecordList = conferenceRecordManage.getByCondition(record);
        if (Objects.isNull(conferenceRecordList) || conferenceRecordList.isEmpty()) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_RECORD_NOT_EXIST);
            return;
        }
        log.info("playbackConferenceRecord rpcConnection userUuid:{}, conference recorder uuid:{}", rpcConnection.getUserUuid(), conferenceRecordList.get(0).getRecorderUuid());
        if (!rpcConnection.getUserUuid().equals(conferenceRecordList.get(0).getRecorderUuid())) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }
        // 回放操作
        conferenceRecordInfoManage.playbackConferenceRecord(conferenceRecordInfo, getUserByRpcConnection(rpcConnection));

        JsonObject jsonObject = new JsonObject();
        String recordUrl;
        if (!StringUtil.isNullOrEmpty(recordUrl = getRecordUrl(conferenceRecordInfo))) {
            jsonObject.addProperty("recordUrl", recordUrl);
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }

    public String getRecordUrl(ConferenceRecordInfo conferenceRecordInfo) {
        String recordName = conferenceRecordInfo.getRecordName();
        String recordUrl = openviduConfig.getRecordPlaybackServer() + recordName.substring(0, recordName.lastIndexOf(".")) + "/" + recordName;
        log.info("PlaybackConferenceRecordHandler getRecordUrl status:" + (StringUtil.isNullOrEmpty(recordUrl) ? "fail" : "success"));
        return recordUrl;
    }
}

