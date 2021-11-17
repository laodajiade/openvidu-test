package io.openvidu.server.common.broker;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.enums.RoomIdTypeEnums;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.ValidPeriodHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CorpServiceExpiredNotifyHandler {

    @Autowired
    CorporationMapper corporationMapper;

    @Resource
    private RpcNotificationService rpcNotificationService;

    @Resource
    private CacheManage cacheManage;

    @Resource
    private ConferenceMapper conferenceMapper;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    protected RpcNotificationService notificationService;

    private static final Object lock = new Object();

    @Async
    public void notify(String message) {
        log.info("corp in modified to notify corpId:" + message);
        final Long corpId = Long.parseLong(message);
        Corporation corporation = corporationMapper.selectByPrimaryKey(corpId);
        if (corporation == null) {
            log.error("corp service expired notify error corp not exist");
            return;
        }
        JsonObject params = new JsonObject();
        params.addProperty("project", corporation.getProject());
        params.addProperty("expireDate", corporation.getExpireDate().format(DateTimeFormatter.ofPattern(DateUtil.DEFAULT_YEAR_MONTH_DAY)));
        params.addProperty("validPeriod", ValidPeriodHelper.getBetween(corporation.getExpireDate()));

        int remainderDuration = cacheManage.getCorpRemainDuration(corporation.getProject());
        int remainderHour = remainderDuration / 60;
        int remainderMinute = remainderDuration % 60;
        params.addProperty("remainderHour", remainderHour);
        params.addProperty("remainderMinute", remainderMinute);
        rpcNotificationService.getRpcConnections().stream()
                .filter(rpcConnection -> !Objects.isNull(rpcConnection) && Objects.equals(rpcConnection.getProject(), corporation.getProject()))
                .forEach(rpcConnection -> rpcNotificationService.sendNotification(rpcConnection.getParticipantPrivateId(),
                        ProtocolElements.CORP_INFO_MODIFIED_NOTIFY_METHOD, params));

        if (!corporationMapper.isConcurrentServiceDuration(corporation)) {
            closeRoomByConcurrent(corporation.getProject(), EndReason.serviceExpired);
        }

        if (remainderDuration <= 0) {
            closeRoomByConcurrent(corporation.getProject(), EndReason.callDurationUsedUp);
        }
    }

    public void closeRoomByConcurrent(String project, EndReason reason) {
        synchronized (lock) {
            final List<Conference> notFinishConferences = conferenceMapper.getNotFinishConference()
                    .stream().filter(conference -> conference.getProject().equals(project)).collect(Collectors.toList());

            for (Conference conference : notFinishConferences) {
                if (!RoomIdTypeEnums.isFixed(conference.getRoomId())) {
                    sessionManager.closeRoom(conference.getRoomId(), reason, true);
                }
            }
        }
    }

}
