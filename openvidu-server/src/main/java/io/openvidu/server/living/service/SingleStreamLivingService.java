package io.openvidu.server.living.service;

import io.openvidu.client.OpenViduException;
import io.openvidu.java.client.LivingProperties;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.living.LiveEndpointWrapper;
import io.openvidu.server.living.Living;
import org.kurento.client.LiveEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingleStreamLivingService extends LivingService {
    private static final Logger log = LoggerFactory.getLogger(SingleStreamLivingService.class);

    private Map<String, Map<String, LiveEndpointWrapper>> activeLivers = new ConcurrentHashMap<>();
    private Map<String, Map<String, LiveEndpointWrapper>> storedLivers = new ConcurrentHashMap<>();

    public SingleStreamLivingService(LivingManager livingManager, OpenviduConfig openviduConfig, CacheManage cacheManage) {
        super(livingManager, openviduConfig, cacheManage);
    }

    @Override
    public Living startLiving(Session session, String creatorUuid, LivingProperties properties) throws OpenViduException {
        log.info("Starting living of session {}", session.getSessionId());

        // Instantiate and store living object
        Living living = new Living(session, "", creatorUuid, properties);
        this.livingManager.startingLivings.put(living.getId(), living);

        activeLivers.put(session.getSessionId(), new ConcurrentHashMap<String, LiveEndpointWrapper>());
        storedLivers.put(session.getSessionId(), new ConcurrentHashMap<String, LiveEndpointWrapper>());

        for (Participant p : session.getParticipants()) {
            if (p.isStreaming()) {

                MediaProfileSpecType profile = null;
                try {
                    profile = generateMediaProfile(properties, p);
                } catch (OpenViduException e) {
                    log.error(
                            "Cannot start single stream live for stream {} in session {}: {}. Skipping to next stream being published",
                            p.getUuid(), session.getSessionId(), e.getMessage());
                    continue;
                }
                try {
                    this.startLiveEndpointForPublisherEndpoint(session, living.getId(), profile, p);
                } catch (Exception e) {
                    log.error("start live failed in session {}", session.getSessionId());
                    throw this.failStartLiving(session, living, e.getMessage());
                }
            }
        }
        return living;
    }

    @Override
    public Living stopLiving(Session session, Living living, EndReason reason) {
        return this.stopRecording(session, living, reason, 0);
    }

    public Living stopRecording(Session session, Living living, EndReason reason, long kmsDisconnectionTime) {
        log.info("Stopping individual ({}) living {} of session {}. Reason: {}",
                living.hasVideo() ? (living.hasAudio() ? "video+audio" : "video-only") : "audioOnly",
                living.getId(), living.getSessionId(), reason);

        final HashMap<String, LiveEndpointWrapper> wrappers = new HashMap<>(
                storedLivers.get(living.getSessionId()));

        for (LiveEndpointWrapper wrapper : wrappers.values()) {
            this.stopLiveEndpointOfPublisherEndpoint(living.getSessionId(), wrapper.getStreamId(), kmsDisconnectionTime);
        }

        this.cleanLivingMaps(session, living);
        this.cleanLivingCache(session);
        storedLivers.remove(session.getSessionId());

        return living;
    }

    public void startLiveEndpointForPublisherEndpoint(Session session, String livingId,
                                                      MediaProfileSpecType profile, Participant participant) {
        log.info("Starting single stream live for stream {} in session {}", participant.getUuid(),
                session.getSessionId());

        if (livingId == null) {
            // Stream is being recorded because is a new publisher in an ongoing recorded
            // session. If recordingId is defined is because Stream is being recorded from
            // "startRecording" method
            Living living = this.livingManager.sessionsLivings.get(session.getSessionId());
            livingId = living.getId();

            try {
                profile = generateMediaProfile(living.getLivingProperties(), participant);
            } catch (OpenViduException e) {
                log.error("Cannot start single stream live for stream {} in session {}: {}",
                        participant.getUuid(), session.getSessionId(), e.getMessage());
                return;
            }
        }

        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        MediaPipeline pipeline = kurentoParticipant.getPublisher(StreamType.MAJOR).getPipeline();

        LiveEndpoint live = new LiveEndpoint.Builder(pipeline, "").build();

        connectAccordingToProfile(kurentoParticipant.getPublisher(StreamType.MAJOR), live, profile);
        PublisherEndpoint publisher = kurentoParticipant.getPublisher(StreamType.MAJOR);
        LiveEndpointWrapper wrapper = new LiveEndpointWrapper(live, participant.getParticipantPublicId(),
                livingId, publisher.getStreamId(), participant.getClientMetadata(),
                participant.getServerMetadata(), publisher.getMediaOptions().hasAudio(),
                publisher.getMediaOptions().hasVideo(),
                publisher.getMediaOptions().getTypeOfVideo());

        activeLivers.get(session.getSessionId()).put(publisher.getStreamId(), wrapper);
        storedLivers.get(session.getSessionId()).put(publisher.getStreamId(), wrapper);
        wrapper.getLiver().startLive();
    }

    public void stopLiveEndpointOfPublisherEndpoint(String sessionId, String streamId, Long kmsDisconnectionTime) {
        log.info("Stopping single stream live for stream {} in session {}", streamId, sessionId);
        final LiveEndpointWrapper finalWrapper = activeLivers.get(sessionId).remove(streamId);
        if (finalWrapper != null && kmsDisconnectionTime == 0) {
            finalWrapper.getLiver().stop();
        } else {
            if (kmsDisconnectionTime != 0) {
                // Stopping live endpoint because of a KMS disconnection
                finalWrapper.setEndTime(kmsDisconnectionTime);
                log.warn("Forcing individual living stop after KMS restart for stream {} in session {}", streamId,
                        sessionId);
            } else {
                if (storedLivers.get(sessionId).containsKey(streamId)) {
                    log.info("Stream {} living of session {} was already stopped", streamId, sessionId);
                } else {
                    log.error("Stream {} wasn't being live in session {}", streamId, sessionId);
                }
            }
        }
    }

    private MediaProfileSpecType generateMediaProfile(LivingProperties properties, Participant participant)
            throws OpenViduException {

        KurentoParticipant kParticipant = (KurentoParticipant) participant;
        MediaProfileSpecType profile = null;

        boolean streamHasAudio = kParticipant.getPublisher(StreamType.MAJOR).getMediaOptions().hasAudio();
        boolean streamHasVideo = kParticipant.getPublisher(StreamType.MAJOR).getMediaOptions().hasVideo();
        boolean propertiesHasAudio = properties.hasAudio();
        boolean propertiesHasVideo = properties.hasVideo();

        if (streamHasAudio) {
            if (streamHasVideo) {
                // Stream has both audio and video tracks

                if (propertiesHasAudio) {
                    if (propertiesHasVideo) {
                        profile = MediaProfileSpecType.WEBM;
                    } else {
                        profile = MediaProfileSpecType.WEBM_AUDIO_ONLY;
                    }
                } else {
                    profile = MediaProfileSpecType.WEBM_VIDEO_ONLY;
                }
            } else {
                // Stream has audio track only

                if (propertiesHasAudio) {
                    profile = MediaProfileSpecType.WEBM_AUDIO_ONLY;
                } else {
                    // ERROR: LivingProperties set to video only but there's no video track
                    throw new OpenViduException(
                            OpenViduException.Code.MEDIA_TYPE_STREAM_INCOMPATIBLE_WITH_LIVING_PROPERTIES_ERROR_CODE,
                            "LivingProperties set to \"hasAudio(false)\" but stream is audio-only");
                }
            }
        } else if (streamHasVideo) {
            // Stream has video track only

            if (propertiesHasVideo) {
                profile = MediaProfileSpecType.WEBM_VIDEO_ONLY;
            } else {
                // ERROR: LivingProperties set to audio only but there's no audio track
                throw new OpenViduException(OpenViduException.Code.MEDIA_TYPE_STREAM_INCOMPATIBLE_WITH_LIVING_PROPERTIES_ERROR_CODE,
                        "LivingProperties set to \"hasVideo(false)\" but stream is video-only");
            }
        } else {
            // ERROR: Stream has no track at all. This branch should never be reachable
            throw new OpenViduException(OpenViduException.Code.MEDIA_TYPE_STREAM_INCOMPATIBLE_WITH_LIVING_PROPERTIES_ERROR_CODE,
                    "Stream has no track at all. Cannot be lived");
        }
        return profile;
    }

    private void connectAccordingToProfile(PublisherEndpoint publisherEndpoint, LiveEndpoint live,
                                           MediaProfileSpecType profile) {
        switch (profile) {
            case WEBM:
                publisherEndpoint.connect(live, MediaType.AUDIO);
                publisherEndpoint.connect(live, MediaType.VIDEO);
                break;
            case WEBM_AUDIO_ONLY:
                publisherEndpoint.connect(live, MediaType.AUDIO);
                break;
            case WEBM_VIDEO_ONLY:
                publisherEndpoint.connect(live, MediaType.VIDEO);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported profile when single stream living: " + profile);
        }
    }
}

