package io.openvidu.server.rpc.handlers;

import com.github.pagehelper.Page;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import io.openvidu.server.common.pojo.RoomRecordSummary;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author chosongi
 * @date 2020/9/10 10:21
 */
@Component
public class GetRoomsRecordInfoHandler extends RpcAbstractHandler {

    @Value("${common.storage.limit}")
    private String commonStorageLimit;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        int pageNum,size;
        ConferenceRecordSearch search = ConferenceRecordSearch.builder()
                .project(rpcConnection.getProject())
                .size(size = getIntParam(request, "size"))
                .pageNum(pageNum = getIntParam(request, "pageNum"))
                .roomIds(roomManage.getSubRoomIds(getStringOptionalParam(request, "roomId"), getLongOptionalParam(request, "orgId")))
                .roomSubject(getStringOptionalParam(request, "roomSubject"))
                .build();

        if (pageNum <= 0 || size <= 0 || size > 100) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        JsonArray roomsRecordArr = new JsonArray();
        Page<RoomRecordSummary> page = conferenceRecordManage.getRoomRecordSummaryByCondition(search);
        for (RoomRecordSummary roomRecordSummary : page.getResult()) {
            JsonObject roomRecord = new JsonObject();
            roomRecord.addProperty("roomId", roomRecordSummary.getRoomId());
            roomRecord.addProperty("roomSubject", roomRecordSummary.getRoomSubject());
            roomRecord.addProperty("occupation",
                    new BigDecimal(roomRecordSummary.getOccupation()).divide(bigDecimalMB, 2, BigDecimal.ROUND_UP).toString());

            roomsRecordArr.add(roomRecord);
        }

        List<RoomRecordSummary> roomRecordSummaries = conferenceRecordManage.getAllRoomRecordSummaryByProject(ConferenceRecordSearch.builder()
                        .project(rpcConnection.getProject()).build());
        long usedSpaceSize = roomRecordSummaries.stream().mapToLong(RoomRecordSummary::getOccupation).sum();
        JsonObject resp = new JsonObject();
        resp.addProperty("totalStorageSpace", commonStorageLimit);
        resp.addProperty("usedStorageSpace",
                new BigDecimal(usedSpaceSize).divide(bigDecimalMB, 2, BigDecimal.ROUND_UP).toString());
        resp.addProperty("total", page.getTotal());
        resp.add("roomsRecordInfo", roomsRecordArr);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), resp);
    }

}
