package io.openvidu.server.common.broker;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.LocalDateUtils;
import io.openvidu.server.utils.ValidPeriodHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Component
@Slf4j
public class CorpServiceExpiredNotifyHandler {

    @Autowired
    CorporationMapper corporationMapper;

    @Resource
    private RpcNotificationService rpcNotificationService;

    @Resource
    private CacheManage cacheManage;

    @Async
    public void notify(String message) {
        log.info("CorpServiceExpiredNotifyHandler corpId:" + message);
        String corpId = message;
        Corporation corporation = corporationMapper.selectByPrimaryKey(Long.parseLong(corpId));
        if (corporation == null) {
            log.error("corp service expired notify error corp not exist");
            return;
        }
        JsonObject params = new JsonObject();
        params.addProperty("project", corporation.getProject());
        params.addProperty("expireDate", corporation.getExpireDate().format(DateTimeFormatter.ofPattern(DateUtil.DEFAULT_YEAR_MONTH_DAY)));
        params.addProperty("validPeriod", ValidPeriodHelper.getBetween(corporation.getExpireDate()));

        int remainderDuration = cacheManage.getCorpRemainDuration(corporation.getProject());
        int remainderHour = remainderDuration/60;
        int remainderMinute = remainderDuration%60;
        params.addProperty("remainderHour",remainderHour);
        params.addProperty("remainderMinute",remainderMinute);

        rpcNotificationService.getRpcConnections().stream()
                .filter(rpcConnection -> !Objects.isNull(rpcConnection) && Objects.equals(rpcConnection.getProject(), corporation.getProject()))
                .forEach(rpcConnection -> rpcNotificationService.sendNotification(rpcConnection.getParticipantPrivateId(),
                        ProtocolElements.CORP_INFO_MODIFIED_NOTIFY_METHOD, params)
                );


    }

}
