package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.dao.FixedRoomMapper;
import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.RoomRecordSummary;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.ValidPeriodHelper;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2020/10/22 19:39
 */
@Service
public class GetCorpInfoHandler extends RpcAbstractHandler {

    @Autowired
    private FixedRoomMapper fixedRoomMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        JsonObject respObj = new JsonObject();
        Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
        if (Objects.nonNull(corporation)) {
            respObj.addProperty("corpCapacity", corporation.getCapacity());

            respObj.addProperty("expireDate", corporation.getExpireDate().format(DateTimeFormatter.ofPattern(DateUtil.DEFAULT_YEAR_MONTH_DAY)));
            respObj.addProperty("validPeriod", ValidPeriodHelper.getBetween(corporation.getExpireDate()));

            respObj.addProperty("totalStorageSpace", conferenceRecordManage.getCorpRecordStorage(rpcConnection.getProject()).toString());
            List<RoomRecordSummary> roomRecordSummaries = conferenceRecordManage.getAllRoomRecordSummaryByProject(
                    ConferenceRecordSearch.builder().project(rpcConnection.getProject()).build());
            long usedSpaceSize = roomRecordSummaries.stream().mapToLong(RoomRecordSummary::getOccupation).sum();
            respObj.addProperty("usedStorageSpace",
                    new BigDecimal(usedSpaceSize).divide(bigDecimalMB, 2, BigDecimal.ROUND_UP).toString());

            respObj.addProperty("remainderHour", corporation.getRemainderDuration() / 60);
            respObj.addProperty("remainderMinute", corporation.getRemainderDuration() % 60);
            respObj.addProperty("fixedRoomCount", fixedRoomMapper.countActivationRoom(rpcConnection.getCorpId()));
        }

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }
}
