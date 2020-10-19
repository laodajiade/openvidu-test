package io.openvidu.server.rpc.handlers;

import com.github.pagehelper.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ConferenceRecordStatusEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.SortEnum;
import io.openvidu.server.common.pojo.ConferenceRecord;
import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GetConferenceRecordHandler extends RpcAbstractHandler {
    @Value("${record.query.time.interval}")
    private Integer recordQueryTimeInterval;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringParam(request, "roomId");
        SortEnum sort = SortEnum.valueOf(getStringParam(request, "sort"));
        ConferenceRecordSearch.SortFilter filter = ConferenceRecordSearch.SortFilter.valueOf(getStringParam(request, "filter"));
        int pageNum = getIntParam(request, ProtocolElements.GET_CONF_RECORD__PAGENUM_PARAM);
        int size = getIntParam(request, ProtocolElements.GET_CONF_RECORD__SIZE_PARAM);
        if (pageNum <= 0 || size <= 0 || size > 100) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        long total = 0L;
        JsonArray jsonArray = new JsonArray();

        // 根据roomId进行查询
        ConferenceRecord record = new ConferenceRecord();
        record.setRoomId(roomId);
        record.setProject(rpcConnection.getProject());
        List<ConferenceRecord> conferenceRecordList = conferenceRecordManage.getByCondition(record);
        if (!CollectionUtils.isEmpty(conferenceRecordList)) {
            // 录制文件处理补偿（当前录制KMS高概率出现无法回调结束录制事件）
            record.setId(conferenceRecordList.get(conferenceRecordList.size() - 1).getId());
            record.setStatus(ConferenceRecordStatusEnum.FINISH.getStatus());
            conferenceRecordManage.updatePreRecordErrorStatus(record);

            ConferenceRecordSearch condition = ConferenceRecordSearch.builder()
                    .ruidList(conferenceRecordList.stream().map(ConferenceRecord::getRuid).distinct().collect(Collectors.toList()))
                    .sort(sort)
                    .filter(filter)
                    .pageNum(pageNum)
                    .size(size).build();

            List<ConferenceRecordInfo> recordInfoList;
            Page<ConferenceRecordInfo> recordInfoPage = conferenceRecordInfoManage.getPageListBySearch(condition);
            if (!CollectionUtils.isEmpty(recordInfoList = recordInfoPage.getResult())) {
                total = recordInfoPage.getTotal();
                recordInfoList.forEach(conferenceRecordInfo -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("id", conferenceRecordInfo.getId());
                    jsonObject.addProperty("recordName", conferenceRecordInfo.getRecordName());
                    jsonObject.addProperty("occupation",
                            new BigDecimal(conferenceRecordInfo.getRecordSize()).divide(bigDecimalMB, 2, BigDecimal.ROUND_UP).toString());
                    jsonObject.addProperty("startTime", conferenceRecordInfo.getStartTime().getTime());
                    jsonObject.addProperty("duration",
                            (conferenceRecordInfo.getEndTime().getTime() - conferenceRecordInfo.getStartTime().getTime()) / 1000);
                    jsonObject.addProperty("status", conferenceRecordInfo.getFinishedStatus());

                    jsonArray.add(jsonObject);
                });
            }
        }

        JsonObject respObj = new JsonObject();
        respObj.addProperty(ProtocolElements.GET_CONF_RECORD__TOTAL_PARAM, total);
        respObj.add(ProtocolElements.GET_CONF_RECORD__RECORDS_PARAM, jsonArray);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }

}


