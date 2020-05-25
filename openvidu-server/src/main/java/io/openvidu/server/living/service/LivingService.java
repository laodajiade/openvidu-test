package io.openvidu.server.living.service;

import io.openvidu.client.OpenViduException;
import io.openvidu.java.client.LivingProperties;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Session;
import io.openvidu.server.living.Living;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LivingService {
    private static final Logger log = LoggerFactory.getLogger(LivingService.class);

    protected LivingManager livingManager;
    protected OpenviduConfig openviduConfig;
    protected CacheManage cacheManage;

    public LivingService(LivingManager livingManager, OpenviduConfig openviduConfig, CacheManage cacheManage) {
        this.livingManager = livingManager;
        this.openviduConfig = openviduConfig;
        this.cacheManage = cacheManage;
    }

    public abstract Living startLiving(Session session, String creatorUuid, LivingProperties properties) throws OpenViduException;

    public abstract Living stopLiving(Session session, Living living, EndReason reason);

    protected OpenViduException failStartLiving(Session session, Living living, String errorMessage) {
        log.error("Living start failed for session {}: {}", session.getSessionId(), errorMessage);
        this.livingManager.startingLivings.remove(living.getId());
        this.stopLiving(session, living, null);

        return new OpenViduException(OpenViduException.Code.LIVING_START_ERROR_CODE, errorMessage);
    }

    protected void cleanLivingMaps(Session session, Living living) {
        log.info("cleanLivingMaps for session:{}", session.getSessionId());
        session.setIsLiving(false);
        this.livingManager.sessionsLivings.remove(living.getSessionId());
        this.livingManager.startedLivings.remove(living.getId());
    }

    protected void cleanLivingCache(Session session) {
        cacheManage.delLivingInfo(session.getSessionId());
    }
}

