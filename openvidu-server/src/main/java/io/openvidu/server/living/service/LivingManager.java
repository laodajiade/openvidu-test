package io.openvidu.server.living.service;

import io.netty.util.internal.StringUtil;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.LivingProperties;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.*;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.living.Living;
import io.openvidu.server.recording.Recording;
import org.kurento.client.MediaProfileSpecType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LivingManager {
    private static final Logger log = LoggerFactory.getLogger(LivingManager.class);

    protected ComposedLivingService composedLivingService;
    protected SingleStreamLivingService singleStreamLivingService;

    @Autowired
    private SessionManager sessionManager;
    @Autowired
    protected OpenviduConfig openviduConfig;
    @Autowired
    protected CacheManage cacheManage;

    protected Map<String, Living> sessionsLivings = new ConcurrentHashMap<>();
    protected Map<String, Living> startingLivings = new ConcurrentHashMap<>();
    protected Map<String, Living> startedLivings = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> automaticLivingStopThreads = new ConcurrentHashMap<>();

    private ScheduledThreadPoolExecutor automaticLivingStopExecutor = new ScheduledThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors());

    public void initializeLivingManager() throws OpenViduException {
        this.composedLivingService = new ComposedLivingService(this, openviduConfig, cacheManage);
        this.singleStreamLivingService = new SingleStreamLivingService(this, openviduConfig, cacheManage);
    }

    public String startLiving(Session session, LivingProperties properties, String creatorUuid) throws OpenViduException {
        Living living = null;
        try {
            switch (properties.outputMode()) {
                case COMPOSED:
                    living = this.composedLivingService.startLiving(session, creatorUuid, properties);
                    break;
                case INDIVIDUAL:
                    living = this.singleStreamLivingService.startLiving(session, creatorUuid, properties);
                    break;
            }
        } catch (OpenViduException e) {
            throw e;
        }
        updateLivingManagerCollections(session, living);

        if (session.getActivePublishers() == 0) {
            // Init automatic recording stop if there are now publishers when starting
            // recording
            log.info("No publisher in session {}. Starting {} seconds countdown for stopping living",
                    session.getSessionId(), this.openviduConfig.getOpenviduLivingAutostopTimeout());
            this.initAutomaticLivingStopThread(session);
        }
        return living.getUrl();
    }

    public Living stopLiving(Session session, String livingId, EndReason reason) throws OpenViduException {
        Living living;
        if (session == null) {
            living = this.startedLivings.get(livingId);
        } else {
            living = this.sessionsLivings.get(session.getSessionId());
        }

        switch (living.getOutputMode()) {
            case COMPOSED:
                composedLivingService.stopLiving(session, living, reason);
                break;
            case INDIVIDUAL:
                singleStreamLivingService.stopLiving(session, living, reason);
                break;
        }
        return living;
    }

    public Living forceStopLiving(Session session, EndReason reason, long kmsDisconnectionTime) {
        Living living;
        living = this.sessionsLivings.get(session.getSessionId());
        switch (living.getOutputMode()) {
            case COMPOSED:
                living = this.composedLivingService.stopLiving(session, living, reason);
                if (living.hasVideo()) {
                    // Evict the recorder participant if composed recording with video
                    this.sessionManager.evictParticipant(
                            session.getParticipantByPublicId(ProtocolElements.RECORDER_PARTICIPANT_PUBLICID), null, null,
                            null);
                }
                break;
            case INDIVIDUAL:
                living = this.singleStreamLivingService.stopLiving(session, living, reason);
                break;
        }
        this.abortAutomaticLivingStopThread(session, reason);
        return living;
    }

    public void connectStreamToExistingLiveComposite(Session session, Participant participant) {
        Living living = this.sessionsLivings.get(session.getSessionId());
        if (living == null) {
            log.error("Cannot start living of new stream {}. Session {} is not being lived",
                    participant.getPublisherStreamId(), session.getSessionId());
            return;
        }

        if (io.openvidu.java.client.Recording.OutputMode.COMPOSED.equals(living.getOutputMode())) {
            this.composedLivingService.joinPublisherEndpointToComposite(session, participant);
        }
    }

    public void disconnectStreamToExistingLiveComposite(Session session, Participant participant) {
        Living living = this.sessionsLivings.get(session.getSessionId());
        if (living == null) {
            log.error("Cannot stop living of existing stream {}. Session {} is not being lived", participant.getPublisherStreamId(),
                    session.getSessionId());
            return;
        }

        if (io.openvidu.java.client.Recording.OutputMode.COMPOSED.equals(living.getOutputMode())) {
            this.composedLivingService.removePublisherEndpointFromComposite(session.getSessionId(), participant.getPublisherStreamId());
        }
        Participant moderator = session.getParticipants().stream().filter(p -> Objects.equals(p.getRole(), OpenViduRole.MODERATOR)).findAny().orElse(null);
        session.leaveRoomSetLayout(participant, moderator.getParticipantPublicId());
        session.invokeKmsConferenceLayout(EndpointTypeEnum.living);
    }

    public void startOneIndividualStreamLiving(Session session, String livingId, MediaProfileSpecType profile,
                                               Participant participant) {
        Living living = this.sessionsLivings.get(session.getSessionId());
        if (living == null) {
            log.error("Cannot start living of new stream {}. Session {} is not being lived",
                    participant.getPublisherStreamId(), session.getSessionId());
        }
        if (io.openvidu.java.client.Recording.OutputMode.INDIVIDUAL.equals(living.getOutputMode())) {
            // Start new LiveEndpoint for this stream
            log.info("Starting new LiveEndpoint in session {} for new stream of participant {}",
                    session.getSessionId(), participant.getParticipantPublicId());
            this.singleStreamLivingService.startLiveEndpointForPublisherEndpoint(session, livingId, profile,
                    participant);
        } else if (io.openvidu.java.client.Recording.OutputMode.COMPOSED.equals(living.getOutputMode())
                && !living.hasVideo()) {
            // Connect this stream to existing Composite recorder
            log.info("Joining PublisherEndpoint to existing Composite in session {} for new stream of participant {}",
                    session.getSessionId(), participant.getParticipantPublicId());
            this.composedLivingService.joinPublisherEndpointToComposite(session, participant);
        }
    }

    public void stopOneIndividualStreamLiving(KurentoSession session, String streamId, long kmsDisconnectionTime) {
        Living living = this.sessionsLivings.get(session.getSessionId());
        if (living == null) {
            log.error("Cannot stop living of existing stream {}. Session {} is not being lived", streamId,
                    session.getSessionId());
        }
        if (io.openvidu.java.client.Recording.OutputMode.INDIVIDUAL.equals(living.getOutputMode())) {
            // Stop specific LiveEndpoint for this stream
            log.info("Stopping LiveEndpoint in session {} for stream of participant {}", session.getSessionId(),
                    streamId);
            this.singleStreamLivingService.stopLiveEndpointOfPublisherEndpoint(session.getSessionId(), streamId, kmsDisconnectionTime);
        } else if (io.openvidu.java.client.Recording.OutputMode.COMPOSED.equals(living.getOutputMode())
                && !living.hasVideo()) {
            // Disconnect this stream from existing Composite live
            log.info("Removing PublisherEndpoint from Composite in session {} for stream of participant {}",
                    session.getSessionId(), streamId);
            this.composedLivingService.removePublisherEndpointFromComposite(session.getSessionId(), streamId);
        }
    }

    public boolean sessionIsBeingLived(String sessionId) {
        return (this.sessionsLivings.get(sessionId) != null);
    }

    public String getLiveCreatorUuid(String sessionId) {
        Living living = sessionsLivings.get(sessionId);
        return Objects.isNull(living) ? null : living.getCreatorUuid();
    }

    public void initAutomaticLivingStopThread(final Session session) {
        final String livingId = this.sessionsLivings.get(session.getSessionId()).getId();
        ScheduledFuture<?> future = this.automaticLivingStopExecutor.schedule(() -> {

            log.info("Stopping living {} after {} seconds wait (no publisher published before timeout)", livingId,
                    this.openviduConfig.getOpenviduLivingAutostopTimeout());

            if (this.automaticLivingStopThreads.remove(session.getSessionId()) != null) {
                if (session.getParticipants().size() == 0 || (session.getParticipants().size() == 1
                        && session.getParticipantByPublicId(ProtocolElements.RECORDER_PARTICIPANT_PUBLICID) != null)) {
                    // Close session if there are no participants connected (except for RECORDER).
                    // This code won't be executed only when some user reconnects to the session
                    // but never publishing (publishers automatically abort this thread)
                    log.info("Closing session {} after automatic stop of living {}", session.getSessionId(),
                            livingId);
                    sessionManager.closeSessionAndEmptyCollections(session, EndReason.automaticStop);
                    sessionManager.showTokens();
                } else {
                    this.stopLiving(session, livingId, EndReason.automaticStop);
                }
            } else {
                // This code is reachable if there already was an automatic stop of a recording
                // caused by not user publishing within timeout after recording started, and a
                // new automatic stop thread was started by last user leaving the session
                log.warn("Living {} was already automatically stopped by a previous thread", livingId);
            }

        }, this.openviduConfig.getOpenviduLivingAutostopTimeout(), TimeUnit.SECONDS);
        this.automaticLivingStopThreads.putIfAbsent(session.getSessionId(), future);
    }

    public boolean abortAutomaticLivingStopThread(Session session, EndReason reason) {
        ScheduledFuture<?> future = this.automaticLivingStopThreads.remove(session.getSessionId());
        if (future != null) {
            boolean cancelled = future.cancel(false);
            if (session.getParticipants().size() == 0 || (session.getParticipants().size() == 1
                    && session.getParticipantByPublicId(ProtocolElements.RECORDER_PARTICIPANT_PUBLICID) != null)) {
                // Close session if there are no participants connected (except for RECORDER).
                // This code will only be executed if recording is manually stopped during the
                // automatic stop timeout, so the session must be also closed
                log.info(
                        "Ongoing living of session {} was explicetly stopped within timeout for automatic living stop. Closing session",
                        session.getSessionId());
                sessionManager.closeSessionAndEmptyCollections(session, reason);
                sessionManager.showTokens();
            }
            return cancelled;
        } else {
            return true;
        }
    }

    private void updateLivingManagerCollections(Session session, Living living) {
        this.sessionsLivings.put(session.getSessionId(), living);
        this.startingLivings.remove(living.getId());
        this.startedLivings.put(living.getId(), living);
    }

    public Living getLiving(String sessionId) {
        if (StringUtil.isNullOrEmpty(sessionId)) return null;
        return sessionsLivings.get(sessionId);
    }
}

