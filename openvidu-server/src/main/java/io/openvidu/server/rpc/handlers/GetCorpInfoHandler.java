package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.RoomRecordSummary;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.LocalDateUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2020/10/22 19:39
 */
@Service
public class GetCorpInfoHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        JsonObject respObj = new JsonObject();
        Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
        if (Objects.nonNull(corporation)) {
            respObj.addProperty("corpCapacity", corporation.getCapacity());

            respObj.addProperty("expireDate",
                    DateUtil.getDateFormat(corporation.getExpireDate(), DateUtil.DEFAULT_YEAR_MONTH_DAY));
            respObj.addProperty("validPeriod",
                    ChronoUnit.DAYS.between(LocalDate.now(), LocalDateUtils.translateFromDate(corporation.getExpireDate())));

            respObj.addProperty("totalStorageSpace", openviduConfig.getCommonStorageLimit());
            List<RoomRecordSummary> roomRecordSummaries = conferenceRecordManage.getAllRoomRecordSummaryByProject(
                    ConferenceRecordSearch.builder().project(rpcConnection.getProject()).build());
            long usedSpaceSize = roomRecordSummaries.stream().mapToLong(RoomRecordSummary::getOccupation).sum();
            respObj.addProperty("usedStorageSpace",
                    new BigDecimal(usedSpaceSize).divide(bigDecimalMB, 2, BigDecimal.ROUND_UP).toString());
        }

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }
}