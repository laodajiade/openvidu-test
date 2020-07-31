package io.openvidu.server.living.service;

import io.openvidu.client.OpenViduException;
import io.openvidu.java.client.LivingProperties;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.EndpointTypeEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.living.Living;
import io.openvidu.server.recording.CompositeWrapper;
import org.kurento.client.internal.server.KurentoServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ComposedLivingService extends LivingService {
    private static final Logger log = LoggerFactory.getLogger(ComposedLivingService.class);

    private Map<String, CompositeWrapper> composites = new ConcurrentHashMap<>();

    ComposedLivingService(LivingManager livingManager, OpenviduConfig openviduConfig, CacheManage cacheManage) {
        super(livingManager, openviduConfig, cacheManage);
    }

    @Override
    public Living startLiving(Session session, String creatorUuid, LivingProperties properties) throws OpenViduException {
        log.info("Starting living of session {}", session.getSessionId());

        // Instantiate and store living object
        Living living = new Living(session, "", creatorUuid, properties);
        this.livingManager.startingLivings.put(living.getId(), living);

        return this.startLivingWithVideo(session, living);
    }

    @Override
    public Living stopLiving(Session session, Living living, EndReason reason) {
        return this.stopLivingWithVideo(session, living, reason);
    }

    void joinPublisherEndpointToComposite(Session session, Participant participant)
            throws OpenViduException {
        log.info("Joining single stream {} to Composite in session {}", participant.getPublisherStreamId(),
                session.getSessionId());

        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        CompositeWrapper compositeWrapper = this.composites.get(session.getSessionId());

        try {
            compositeWrapper.connectPublisherEndpoint(kurentoParticipant.getPublisher(), EndpointTypeEnum.living);
            // Multiplex MCU layout
            session.dealParticipantDefaultOrder(kurentoParticipant, EndpointTypeEnum.living);
        } catch (OpenViduException e) {
            if (OpenViduException.Code.LIVING_START_ERROR_CODE.getValue() == e.getCodeValue()) {
                // First user publishing triggered RecorderEnpoint start, but it failed
                throw e;
            }
        }
    }

    void removePublisherEndpointFromComposite(String sessionId, String streamId) {
        CompositeWrapper compositeWrapper = this.composites.get(sessionId);
        compositeWrapper.disconnectPublisherEndpoint(streamId);
        if (compositeWrapper.getHubPorts().isEmpty() || compositeWrapper.getPublisherEndpoints().isEmpty()) {
            log.warn("THERE IS NO MORE hubPorts and publisherEndPoints in '{}' and composite id :{}",
                    sessionId, compositeWrapper.getComposite().getId());
        }
    }

    private Living startLivingWithVideo(Session session, Living living) throws OpenViduException {
        log.info("Starting composed video living {} of session {}", living.getId(), living.getSessionId());

        CompositeWrapper compositeWrapper = null;
        try {
            compositeWrapper = new CompositeWrapper((KurentoSession) session,
                    this.openviduConfig.getOpenViduLivingPath() + session.getSessionId() + "_" + System.currentTimeMillis(),
                    EndpointTypeEnum.living);
        } catch (KurentoServerException e) {
            log.error("Error create Live CompositeWrapper in session {}", session.getSessionId());
            throw this.failStartLiving(session, living, e.getMessage());
        }

        this.composites.put(session.getSessionId(), compositeWrapper);

        for (Participant p : session.getParticipants()) {
            if (p.isStreaming()) {
                try {
                    this.joinPublisherEndpointToComposite(session, p);
                } catch (OpenViduException e) {
                    log.error("Error waiting for LiveEndpoint of Composite to start in session {}",
                            session.getSessionId());
                    throw this.failStartLiving(session, living, e.getMessage());
                }
            }
        }

        String livingUrl = getLivingUrl(compositeWrapper.getLiveEndPointUri());
        living.setUrl(livingUrl);
        log.info("Living of session {} started! livingUrl:{}", session.getSessionId(), livingUrl);
        return living;
    }

    private Living stopLivingWithVideo(Session session, Living living, EndReason reason) throws OpenViduException {
        log.info("Stopping living of session {}. Reason: {}", session.getSessionId(), reason);

        String sessionId;
        if (session == null) {
            log.warn(
                    "Existing living {} does not have an active session associated. This means the living "
                            + "has been automatically stopped after last user left and {} seconds timeout passed",
                    living.getId(), this.openviduConfig.getOpenviduRecordingAutostopTimeout());
            sessionId = living.getSessionId();
        } else {
            sessionId = session.getSessionId();
        }

        CompositeWrapper compositeWrapper = this.composites.remove(sessionId);
        if (null != compositeWrapper) {
            compositeWrapper.stopCompositeLiving();
            compositeWrapper.disconnectAllPublisherEndpoints();
        }
        log.info("Living of session {} stopped!", session.getSessionId());
        this.cleanLivingMaps(session, living);
        this.cleanLivingCache(session);
        return living;
    }

    public String getLivingUrl(String str) {
        String rtmp = "rtmp://";
        String suffix = str.substring(str.indexOf("/", rtmp.length()) + 1);
        String url = this.openviduConfig.getOpenviduLivingHttpUrlPrefix() + suffix + ".m3u8";
        return url;
    }
}
