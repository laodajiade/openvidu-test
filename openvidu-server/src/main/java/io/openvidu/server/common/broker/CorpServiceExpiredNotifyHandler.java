package io.openvidu.server.common.broker;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.RoomIdTypeEnums;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.utils.DateUtil;
import io.openvidu.server.utils.ValidPeriodHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;

@Component
@Slf4j
public class CorpServiceExpiredNotifyHandler {

    @Autowired
    CorporationMapper corporationMapper;

    @Resource
    private RpcNotificationService rpcNotificationService;

    @Resource
    private CacheManage cacheManage;

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    protected RpcNotificationService notificationService;

    @Async
    public void notify(String message) {
        log.info("corp in modified to notify corpId:" + message);
        final String corpId = message;
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
        int remainderHour = remainderDuration / 60;
        int remainderMinute = remainderDuration % 60;
        params.addProperty("remainderHour", remainderHour);
        params.addProperty("remainderMinute", remainderMinute);

        rpcNotificationService.getRpcConnections().stream()
                .filter(rpcConnection -> !Objects.isNull(rpcConnection) && Objects.equals(rpcConnection.getProject(), corporation.getProject()))
                .forEach(rpcConnection -> rpcNotificationService.sendNotification(rpcConnection.getParticipantPrivateId(),
                        ProtocolElements.CORP_INFO_MODIFIED_NOTIFY_METHOD, params)
                );

        if (!corporationMapper.isConcurrentServiceDuration(corporation)) {
            closeRoomByConcurrent(corporation.getProject());
        }
    }

    public void closeRoomByConcurrent(String project) {
        Collection<Session> sessions = sessionManager.getSessions();

        for (Session session : sessions) {
            // close room and notify
            if (Objects.equals(project, session.getConference().getProject())) {
                if (!RoomIdTypeEnums.isFixed(session.getSessionId())) {
                    closeRoomAndNotify(session, EndReason.serviceExpired);
                }
            }
        }
    }

    public void closeRoomAndNotify(Session session, EndReason reason) {
        JsonObject params = new JsonObject();
        params.addProperty("reason", reason.name());

        Set<Participant> participants = sessionManager.getParticipants(session.getSessionId());

        participants.forEach(p -> {
            if (!Objects.equals(StreamType.MAJOR, p.getStreamType())) {
                return;
            }
            notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, params);
            RpcConnection rpcConnect = notificationService.getRpcConnection(p.getParticipantPrivateId());
            if (!Objects.isNull(rpcConnect) && !Objects.isNull(rpcConnect.getSerialNumber())) {
                cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
            }
        });

        sessionManager.stopRecording(session.getSessionId());
        sessionManager.closeSession(session.getSessionId(), EndReason.serviceExpired);
    }

}
