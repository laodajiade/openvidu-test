package io.openvidu.server.common.broker;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.LocalDateUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@Slf4j
public class CorpServiceExpiredNotifyHandler {

    @Autowired
    CorporationMapper corporationMapper;

    @Resource
    private RpcNotificationService rpcNotificationService;

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
        params.addProperty("expireDate", DateUtil.getDateFormat(corporation.getExpireDate(), DateUtil.DEFAULT_YEAR_MONTH_DAY));
        params.addProperty("validPeriod", ChronoUnit.DAYS.between(LocalDate.now(), LocalDateUtils.translateFromDate(corporation.getExpireDate())));

        rpcNotificationService.getRpcConnections().stream()
                .filter(rpcConnection -> rpcConnection.getProject().equals(corporation.getProject()))
                .forEach(rpcConnection -> rpcNotificationService.sendNotification(rpcConnection.getParticipantPrivateId(),
                        ProtocolElements.CORP_INFO_MODIFIED_NOTIFY_METHOD, params)
                );


    }

}
