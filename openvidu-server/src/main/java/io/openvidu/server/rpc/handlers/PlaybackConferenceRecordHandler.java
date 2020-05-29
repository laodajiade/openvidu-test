package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.netty.util.internal.StringUtil;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.AccessTypeEnum;
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

        if (!isAdmin(rpcConnection.getUserUuid())) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }
        // 回放操作
        conferenceRecordInfoManage.playbackConferenceRecord(conferenceRecordInfo, getUserByRpcConnection(rpcConnection));

        JsonObject jsonObject = new JsonObject();
        String recordUrl = Objects.equals(AccessTypeEnum.web, rpcConnection.getAccessType()) ? getWebRecordUrl(conferenceRecordInfo) : getRecordUrl(conferenceRecordInfo);
        if (!StringUtil.isNullOrEmpty(recordUrl)) {
            jsonObject.addProperty("recordUrl", recordUrl);
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }

    public String getRecordUrl(ConferenceRecordInfo conferenceRecordInfo) {
        String recordUrl = openviduConfig.getRecordPlaybackServer() + conferenceRecordInfo.getRecordUrl().replace("/opt/openvidu/recordings/", "");
        log.info("PlaybackConferenceRecordHandler getRecordUrl status:" + (StringUtil.isNullOrEmpty(recordUrl) ? "fail" : "success"));
        return recordUrl;
    }

    private String getWebRecordUrl(ConferenceRecordInfo conferenceRecordInfo) {
        String recordUrl = null;
        JSONObject respObj = httpUtil.syncRequest(openviduConfig.getRecordTranscodingRequestUrl(),
                constructDownloadReqBody(conferenceRecordInfo));
        if (Objects.nonNull(respObj) && respObj.containsKey("hlsVodAddr")) {
            recordUrl = openviduConfig.getRecordThumbnailServer() + respObj.getString("hlsVodAddr").replace("/opt/openvidu/recordings/", "");
        }
        log.info("PlaybackConferenceRecordHandler getWebRecordUrl result:{}", recordUrl);
        return recordUrl;
    }

    private String constructDownloadReqBody(ConferenceRecordInfo conferenceRecordInfo) {
        JSONObject reqBody = new JSONObject();
        reqBody.fluentPut("fileAddr", conferenceRecordInfo.getRecordUrl())
                .fluentPut("outputAddr", conferenceRecordInfo.getRecordUrl().substring(0,
                        conferenceRecordInfo.getRecordUrl().lastIndexOf("/")))
                .fluentPut("remuxMp4", false)
                .fluentPut("screenshot", false)
                .fluentPut("indexFile", false)
                .fluentPut("hlsVod", true)
                .fluentPut("extend", "playbackConferenceRecord");
        return reqBody.toString();
    }
}

