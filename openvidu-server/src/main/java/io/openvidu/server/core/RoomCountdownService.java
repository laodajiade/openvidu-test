package io.openvidu.server.core;

import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@EnableScheduling
@Service
public class RoomCountdownService {
    private static final Logger log = LoggerFactory.getLogger(RoomCountdownService.class);

    @Resource
    private RpcAbstractHandler rpcAbstractHandler;

    @Resource
    protected OpenviduConfig openviduConfig;

    private RpcHandler handler;

    public void setHandler(RpcHandler handler) { this.handler = handler; }

    @Scheduled(fixedRate = 1000, initialDelay = 0)
    public void roomCountDown() {
        SessionManager sessionManager = handler.getSessionManager();
        sessionManager.getSessions().forEach(s -> {
            String sessionId = s.getSessionId();
            long confStartTime = s.getConfStartTime();
            long confEndTime = s.getConfEndTime();
            long remainTime = s.getConfRemainTime();
            int voipCountdownLongTime = openviduConfig.getVoipCountdownLongTime();
            int voipCountdownShortTime = openviduConfig.getVoipCountdownShortTime();

//            log.info("sessionId:{} remainTime:{} startTime:{} confEndTime:{}", sessionId, remainTime, confStartTime, confEndTime);
            if (remainTime <= 0) {
                log.info("session:{} no have remain time. should be closed.", sessionId);
                rpcAbstractHandler.cleanSession(sessionId, "", false, EndReason.forceCloseSessionByUser);
            } else if (remainTime <= voipCountdownShortTime * 60) {
                if (!s.getNotifyCountdown1Min()) {
                    handler.notifyRoomCountdown(sessionId, voipCountdownShortTime);
                    s.setNotifyCountdown1Min(true);
                    log.info("remain {} min, remainTime:{} s", voipCountdownShortTime, remainTime);
                }
            } else if (remainTime <= voipCountdownLongTime * 60) {
                if (!s.getNotifyCountdown10Min()) {
                    handler.notifyRoomCountdown(sessionId, voipCountdownLongTime);
                    s.setNotifyCountdown10Min(true);
                    log.info("remain {} min, remainTime:{} s", voipCountdownLongTime, remainTime);
                }
            } else {
//                log.info("remain many time, remainTime:{}", remainTime);
                s.setNotifyCountdown10Min(false);
                s.setNotifyCountdown1Min(false);
            }
        });
    }
}
