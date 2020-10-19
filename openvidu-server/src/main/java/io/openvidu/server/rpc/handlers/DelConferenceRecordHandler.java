package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DelConferenceRecordHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        List<Long> ids = getLongListParam(request, ProtocolElements.DEL_CONF_RECORD_ID_PARAM);

        if (!isAdmin(rpcConnection.getUserUuid())) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        // Finished Status：1录制中，2录制完成
        List<ConferenceRecordInfo> conferenceRecordInfos;
        if (!CollectionUtils.isEmpty(conferenceRecordInfos = conferenceRecordInfoManage.selectByIds(ids)
                .stream()
                .filter(conferenceRecordInfo -> conferenceRecordInfo.getFinishedStatus().equals(2))
                .collect(Collectors.toList()))) {

            // 删除操作
            conferenceRecordManage.deleteConferenceRecord(conferenceRecordInfos);
        }



        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}