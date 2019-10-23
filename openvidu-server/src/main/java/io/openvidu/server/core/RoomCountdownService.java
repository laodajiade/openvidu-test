package io.openvidu.server.core;

import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@EnableScheduling
@Service
public class RoomCountdownService {
    private static final Logger log = LoggerFactory.getLogger(RoomCountdownService.class);

    private RpcHandler handler;

    public void setHandler(RpcHandler handler) { this.handler = handler; }

    @Scheduled(fixedRate = 1000, initialDelay = 0)
    public void roomCountDown() {
        SessionManager sessionManager = handler.getSessionManager();
        sessionManager.getSessions().forEach(s -> {
            String sessionId = s.getSessionId();
            Date confStartTime = s.getConference().getStartTime();
            int confDuration = Float.valueOf(s.getPresetInfo().getRoomDuration() * 60 * 60 * 1000).intValue() + s.getConfDelayTime() * 1000;
            long confEndTime = confStartTime.getTime() + confDuration;
            long now = new Date().getTime();
            long remainTime = (confEndTime - now) / 1000;

//            log.info("sessionId:{} remainTime:{} s", sessionId, remainTime);
            if (remainTime <= 0) {
                // TODO. wait closeRoom or clean exception resource.
                log.info("session:{} no have remain time. should be closed.", sessionId);
                handler.cleanSession(sessionId, "", false, EndReason.forceCloseSessionByUser);
            } else if (remainTime <= 1 * 60) {
                // TODO. Notify remain 1 min
                if (!s.getNotifyCountdown1Min()) {
                    handler.notifyRoomCountdown(sessionId, 1);
                    s.setNotifyCountdown1Min(true);
                    log.info("remain 1 min, remainTime:{} s", remainTime);
                }
            } else if (remainTime <= 10 * 60) {
                // TODO. Notfiy remain 10 min
                if (!s.getNotifyCountdown10Min()) {
                    handler.notifyRoomCountdown(sessionId, 10);
                    s.setNotifyCountdown10Min(true);
                    log.info("remain 10 min, remainTime:{} s", remainTime);
                }
            } else {
//                log.info("remain many time, remainTime:{}", remainTime);
                s.setNotifyCountdown10Min(false);
                s.setNotifyCountdown1Min(false);
            }
        });
    }
}
