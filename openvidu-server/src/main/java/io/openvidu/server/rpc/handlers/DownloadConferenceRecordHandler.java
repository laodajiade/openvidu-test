package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
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
public class DownloadConferenceRecordHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Long id = getLongParam(request, ProtocolElements.DOWNLOAD_CONF_RECORD_ID_PARAM);

        ConferenceRecordInfo conferenceRecordInfo = conferenceRecordInfoManage.selectByPrimaryKey(id);
        if (Objects.isNull(conferenceRecordInfo)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_RECORD_NOT_EXIST);
            return;
        }
        // 权限校验（非本人会议室不可下载）
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
        // 下载操作
        conferenceRecordInfoManage.downloadConferenceRecord(conferenceRecordInfo, getUserByRpcConnection(rpcConnection));

        JsonObject jsonObject = new JsonObject();
        String downloadUrl;
        if (!StringUtil.isNullOrEmpty(downloadUrl = getDownloadUrl(conferenceRecordInfo))) {
            jsonObject.addProperty("downloadUrl", downloadUrl);
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);

    }

    private String getDownloadUrl(ConferenceRecordInfo conferenceRecordInfo) {
        String downloadUrl = null;
        JSONObject respObj = httpUtil.syncRequest(openviduConfig.getRecordTranscodingRequestUrl(),
                constructDownloadReqBody(conferenceRecordInfo));
        if (Objects.nonNull(respObj) && respObj.containsKey("mp4FileAddr")) {
            downloadUrl = openviduConfig.getRecordThumbnailServer() + respObj.getString("mp4FileAddr").replace("/opt/openvidu/recordings/", "");
        }
        log.info("DownloadConferenceRecordHandler getDownloadUrl result:{}", downloadUrl);
        return downloadUrl;
    }

    private String constructDownloadReqBody(ConferenceRecordInfo conferenceRecordInfo) {
        JSONObject reqBody = new JSONObject();
        reqBody.fluentPut("fileAddr", conferenceRecordInfo.getRecordUrl())
                .fluentPut("outputAddr", conferenceRecordInfo.getRecordUrl().substring(0,
                        conferenceRecordInfo.getRecordUrl().lastIndexOf("/")))
                .fluentPut("remuxMp4", true)
                .fluentPut("screenshot", false)
                .fluentPut("indexFile", false)
                .fluentPut("extend", "downloadConferenceRecord");
        return reqBody.toString();
    }
}

