package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class GetMeetingRecordsHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        int size = getIntParam(request, ProtocolElements.GETMEETINGSRECORDS_SIZE_PARAM);
        int pageNum = getIntParam(request, ProtocolElements.GETMEETINGSRECORDS_PAGENUM_PARAM);
        if (size <= 0 || size > 100 || pageNum <= 0) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }
        String roomId = getStringOptionalParam(request, ProtocolElements.GETMEETINGSRECORDS_ROOMID_PARAM);
        Long dateFrom = getLongOptionalParam(request, ProtocolElements.GETMEETINGSRECORDS_FROM_PARAM);
        Long dateTo = getLongOptionalParam(request, ProtocolElements.GETMEETINGSRECORDS_TO_PARAM);

        ConferenceSearch search = new ConferenceSearch();
        search.setProject(rpcConnection.getProject());
        search.setStatus(ConferenceStatus.FINISHED.getStatus());
        search.setRoomId(roomId);
        search.setLimit(size);
        search.setOffset((pageNum - 1) * size);
        if (!Objects.isNull(dateFrom)) search.setFrom(new Date(dateFrom));
        if (!Objects.isNull(dateTo)) search.setTo(new Date(dateTo));
        List<Conference> records = conferenceMapper.selectPageRecordsByCondition(search);

        JsonObject respObj = new JsonObject();
        JsonArray recordArr = new JsonArray();
        if (!CollectionUtils.isEmpty(records)) {
            records.forEach(record -> {
                JsonObject recObj = new JsonObject();
                recObj.addProperty("ruid", record.getRuid());
                recObj.addProperty("roomId", record.getRoomId());
                recObj.addProperty("subject", record.getConferenceSubject());
                recObj.addProperty("conferenceMode", ConferenceModeEnum.parse(record.getConferenceMode()).name());
                recObj.addProperty("startTime", !Objects.isNull(record.getStartTime()) ? record.getStartTime().getTime() : 0L);
                recObj.addProperty("endTime", !Objects.isNull(record.getEndTime()) ? record.getEndTime().getTime() : 0L);
                recObj.addProperty("concurrentNumber", record.getConcurrentNumber());

                recordArr.add(recObj);
            });
        }

        long totalCount = conferenceMapper.selectConfRecordsCountByCondition(search);
        respObj.addProperty("total", totalCount);
        respObj.add("records", recordArr);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }
}
