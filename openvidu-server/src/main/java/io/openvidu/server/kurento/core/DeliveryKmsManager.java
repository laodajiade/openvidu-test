package io.openvidu.server.kurento.core;

import io.openvidu.client.OpenViduException;
import io.openvidu.server.common.enums.DeliveryKmsStateEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.endpoint.MediaChannel;
import io.openvidu.server.kurento.kms.Kms;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class DeliveryKmsManager {

    @Getter
    private final Kms kms;
    @Getter
    private final String id;

    private final String sessionId;

    private final KurentoSession kSession;

    @Setter
    public MediaPipeline pipeline;

    @Setter
    @Getter
    public CountDownLatch pipelineLatch = new CountDownLatch(1);

    public Throwable pipelineCreationErrorCause;
    private final KurentoSessionEventsHandler kurentoSessionHandler;

    @Getter
    private DeliveryKmsStateEnum state;

    /**
     * key = participant.uuid_publishId
     * value = mediaChannel
     */
    @Getter
    private final Map<String, MediaChannel> dispatcherMap = new ConcurrentHashMap<>();

    private static final Object releasePipelineLock = new Object();

    public DeliveryKmsManager(Kms kms, Session session, KurentoSessionEventsHandler kurentoSessionHandler) {
        this.kms = kms;
        this.kSession = (KurentoSession) session;
        this.sessionId = session.getSessionId();
        this.id = kms.getId() + "_" + session.getSessionId();
        this.kurentoSessionHandler = kurentoSessionHandler;
        state = DeliveryKmsStateEnum.CONNECTING;
    }


    public MediaPipeline getPipeline() {
        try {
            pipelineLatch.await(KurentoSession.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this.pipeline;
    }

    public void release() {
        synchronized (releasePipelineLock) {
            if (pipeline == null) {
                return;
            }
            state = DeliveryKmsStateEnum.CLOSE;

            getPipeline().release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) {
                    log.debug("SESSION {}: Released Pipeline", sessionId);
                    pipeline = null;
                    pipelineLatch = new CountDownLatch(1);
                    pipelineCreationErrorCause = null;
                }

                @Override
                public void onError(Throwable cause) {
                    log.warn("SESSION {}: Could not successfully release Pipeline", sessionId, cause);
                    pipeline = null;
                    pipelineLatch = new CountDownLatch(1);
                    pipelineCreationErrorCause = null;
                }
            });
        }
    }

    public void dispatcher() {
        log.info("begin kms dispatcher sessionId {}", kSession.getSessionId());

        Set<Participant> participants = kSession.getParticipants();

        List<KurentoParticipant> publisher = new ArrayList<>();

        for (Participant participant : participants) {

            if (participant.getRole().needToPublish()) {
                publisher.add((KurentoParticipant) participant);
            }
        }
        String dispatchInfo = publisher.stream().map(p -> p.getUuid() + "(" + p.getPublisherStreamId() + ")").collect(Collectors.joining(","));
        log.info("delivery media sessionId = {}，dispatch info {}", sessionId, dispatchInfo);
        publisher.stream().parallel().forEach(this::dispatcher);
        state = DeliveryKmsStateEnum.READY;
    }

    public MediaChannel dispatcher(KurentoParticipant kParticipant) {
        MediaChannel mediaChannel = new MediaChannel(kSession.getPipeline(), kParticipant.getPublisher().getPassThru(), this.getPipeline(),
                true, kParticipant, kParticipant.getPublisherStreamId(), kSession.getOpenviduConfig(), this);
        MediaChannel oldMediaChannel = kParticipant.getPublisher().getMediaChannels().putIfAbsent(this.getId(), mediaChannel);
        log.info("开始分发 {}, {}", kParticipant.getUuid(), mediaChannel.getId());
        if (oldMediaChannel != null) {
            log.info("participant {} 已创建通道 {}", kParticipant.getUuid(), this.id);
            return oldMediaChannel;
        }
        this.dispatcherMap.put(kParticipant.getPublisherStreamId(), mediaChannel);
        mediaChannel.createChannel();
        kParticipant.getPublisher().getMediaChannels().put(this.getId(), mediaChannel);
        log.info("dispatcherMap {}", dispatcherMap);
        return mediaChannel;
    }

    public void initToReady() {
        this.createPipeline();
        this.dispatcher();
        log.info("delivery kms ready");
    }

    public void createPipeline() {
        log.info("SESSION {}: Creating delivery MediaPipeline,kmsIp ({} >> {}),master kms {}", sessionId, kms.getIp(), kms.getId(), kSession.getKms().getIp());
        try {
            kms.getKurentoClient().createMediaPipeline(new Continuation<MediaPipeline>() {
                @Override
                public void onSuccess(MediaPipeline result) {
                    pipeline = result;
                    pipelineLatch.countDown();
                    state = DeliveryKmsStateEnum.CONNECTED;
                    pipeline.setName(MessageFormat.format("delivery_pipeline_{0}_{1}", kms.getIp(), sessionId));
                    log.info("SESSION {}: Created MediaPipeline {}, MediaPipelineName {} in kmsId {}", sessionId, result.getId(), result.getName(), kms.getId());
                }

                @Override
                public void onError(Throwable cause) {
                    pipelineCreationErrorCause = cause;
                    pipelineLatch.countDown();
                    log.error("SESSION {}: Failed to create MediaPipeline", sessionId, cause);
                }
            });
        } catch (Exception e) {
            log.error("Unable to create media pipeline for session '{}'", sessionId, e);
            pipelineLatch.countDown();
        }
        if (getPipeline() == null) {
            log.info("create pipeline error,deliveryKmsId {}", this.id);
            final String message = pipelineCreationErrorCause != null
                    ? pipelineCreationErrorCause.getLocalizedMessage()
                    : "Unable to create media pipeline for session '" + sessionId + "'";
            pipelineCreationErrorCause = null;
            throw new OpenViduException(OpenViduException.Code.ROOM_CANNOT_BE_CREATED_ERROR_CODE, message);
        }

        pipeline.addErrorListener(event -> {
            String desc = event.getType() + ": " + event.getDescription() + "(errCode=" + event.getErrorCode()
                    + ")";
            log.warn("SESSION {}: Pipeline error encountered: {}", sessionId, desc);
            kurentoSessionHandler.onPipelineError(sessionId, kSession.getParticipants(), desc);
        });

        log.info("delivery kms create success");
    }

    public MediaChannel getMediaChannel(String sendName) {
        return dispatcherMap.get(sendName);
    }
}
