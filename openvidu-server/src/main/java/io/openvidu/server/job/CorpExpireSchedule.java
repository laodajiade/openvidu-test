package io.openvidu.server.job;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import org.apache.commons.lang.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CorpExpireSchedule {

    @Resource
    private CorporationMapper corporationMapper;

    @Resource
    private SessionManager sessionManager;

    @Autowired
    protected RpcNotificationService notificationService;
    @Resource
    protected CacheManage cacheManage;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void doProcess() {
        List<Corporation> corporations = corporationMapper.listCorpExpire(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));

        if (corporations.isEmpty()) {
            cacheManage.dropCorpExpiredCollect();
            return;
        }

        Set<String> expiredSet = corporations.stream().map(Corporation::getProject).collect(Collectors.toSet());

        cacheManage.setCorpExpired(expiredSet);

        Collection<Session> sessions = sessionManager.getSessions();

        for (Session session : sessions) {
            String project = session.getConference().getProject();

            // close room and notify
            if (expiredSet.contains(project)) {
                JsonObject params = new JsonObject();
                params.addProperty("reason", "ServiceExpired");

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

                sessionManager.closeSession(session.getSessionId(), EndReason.serviceExpired);
            }
        }
    }

}
