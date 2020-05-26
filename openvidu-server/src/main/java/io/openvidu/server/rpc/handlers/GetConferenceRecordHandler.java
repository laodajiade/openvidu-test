package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.ConferenceRecord;
import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GetConferenceRecordHandler extends RpcAbstractHandler {
    @Value("${record.query.time.interval}")
    private Integer recordQueryTimeInterval;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        JsonObject respObj = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        respObj.addProperty(ProtocolElements.GET_CONF_RECORD__TOTAL_PARAM, 0);
        respObj.add(ProtocolElements.GET_CONF_RECORD__RECORDS_PARAM, jsonArray);

        int pageNum = getIntParam(request, ProtocolElements.GET_CONF_RECORD__PAGENUM_PARAM);
        int size = getIntParam(request, ProtocolElements.GET_CONF_RECORD__SIZE_PARAM);
        if (pageNum <= 0 || size <= 0 || size > 100) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        // 根据roomId进行查询
        ConferenceRecord record = new ConferenceRecord();
        record.setProject(rpcConnection.getProject());
        List<ConferenceRecord> conferenceRecordList = conferenceRecordManage.getByCondition(record);

        if (Objects.isNull(conferenceRecordList) || conferenceRecordList.isEmpty()) {
            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
            return;
        }
        // 获取ruid并排序
        List<String> ruidList = conferenceRecordList.stream().sorted(Comparator.comparing(ConferenceRecord::getRequestStartTime).reversed()).map(ConferenceRecord::getRuid).distinct().collect(Collectors.toList());
        // 根据ruid获取所有的会议记录
        ConferenceRecordSearch condition = ConferenceRecordSearch.builder().ruidList(ruidList).recordQueryTimeInterval(recordQueryTimeInterval).offset((pageNum - 1) * size).limit(size).build();
        List<ConferenceRecordInfo> infoList = conferenceRecordInfoManage.getPageListBySearch(condition);
        List<String> infoRuidList = infoList.stream().map(ConferenceRecordInfo::getRuid).distinct().collect(Collectors.toList());
        // 获取总数
        long totalCount = conferenceRecordInfoManage.selectConfRecordsInfoCountByCondition(condition);

        ruidList.retainAll(infoRuidList);

        if (Objects.isNull(ruidList) || ruidList.isEmpty()) {
            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
            return;
        }

        ruidList.stream().forEach(ruid -> {
            // 根据ruid获取对应的info，并排序
            List<ConferenceRecordInfo> recordInfoList = infoList.stream().filter(conferenceRecordInfo -> ruid.equals(conferenceRecordInfo.getRuid()))
                    .sorted(Comparator.comparing(ConferenceRecordInfo::getStartTime).reversed()).collect(Collectors.toList());
            recordInfoList.stream().forEach(conferenceRecordInfo -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("ruid", ruid);
                jsonObject.addProperty("id", conferenceRecordInfo.getId());
                jsonObject.addProperty("recordName", conferenceRecordInfo.getRecordDisplayName());
                jsonObject.addProperty("recordSize", conferenceRecordInfo.getRecordSize());
                jsonObject.addProperty("thumbnailUrl", openviduConfig.getRecordThumbnailServer() +
                        conferenceRecordInfo.getThumbnailUrl().replace("/opt/openvidu/recordings/", ""));
                jsonObject.addProperty("startTime", conferenceRecordInfo.getStartTime().getTime());
                jsonObject.addProperty("endTime", conferenceRecordInfo.getEndTime().getTime());
                ConferenceRecord record1 = conferenceRecordList.stream().filter(conferenceRecord -> ruid.equals(conferenceRecord.getRuid())).findFirst().orElse(null);
                jsonObject.addProperty("requestStartTime", Objects.isNull(record1) ? null : record1.getRequestStartTime().getTime());
                jsonArray.add(jsonObject);
            });
        });

        respObj.addProperty(ProtocolElements.GET_CONF_RECORD__TOTAL_PARAM, totalCount);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }

}


