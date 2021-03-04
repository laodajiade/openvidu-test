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
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.MediaPipeline;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DeliveryKmsManager {

    @Getter
    private final Kms kms;
    @Getter
    private final String id;

    private final String sessionId;

    private final Session session;

    @Setter
    public MediaPipeline pipeline;

    @Setter
    @Getter
    public CountDownLatch pipelineLatch = new CountDownLatch(1);

    public Throwable pipelineCreationErrorCause;
    private KurentoSessionEventsHandler kurentoSessionHandler;

    @Getter
    private DeliveryKmsStateEnum state;

    /**
     * key = participant.uuid_publishId
     * value = mediaChannel
     */
    public Map<String, MediaChannel> dispatcherMap = new ConcurrentHashMap<>();

    public DeliveryKmsManager(Kms kms, Session session, KurentoSessionEventsHandler kurentoSessionHandler) {
        this.kms = kms;
        this.session = session;
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
        pipeline.release();
        state = DeliveryKmsStateEnum.FAILED;
    }

    public void dispatcher() {
        log.info("begin kms dispatcher sessionId {}", session.getSessionId());

        Set<Participant> participants = session.getParticipants();

        List<KurentoParticipant> publisher = new ArrayList<>();

        for (Participant participant : participants) {

            if (participant.getRole().needToPublish()) {
                publisher.add((KurentoParticipant) participant);
            }
        }
        log.debug("要分发的part的个数 {}", publisher.size());
        publisher.stream().parallel().forEach(this::dispatcher);
        state = DeliveryKmsStateEnum.READY;
    }

    public MediaChannel dispatcher(KurentoParticipant kParticipant) {

        KurentoSession kSession = (KurentoSession) session;
        MediaChannel mediaChannel = new MediaChannel(kSession.getPipeline(), kParticipant.getPublisher().getPassThru(), this.getPipeline(),
                true, kParticipant, kParticipant.getPublisherStreamId(), kSession.getOpenviduConfig());
        MediaChannel oldMediaChannel = kParticipant.getMediaChannels().putIfAbsent(this.getId(), mediaChannel);
        log.info("开始分发 {}, {}", kParticipant.getUuid(), mediaChannel.getId());
        if (oldMediaChannel != null) {
            log.info("participant {} 已创建通道 {}", kParticipant.getUuid(), this.id);
            return oldMediaChannel;
        }
        this.dispatcherMap.put(kParticipant.getPublisherStreamId(), mediaChannel);
        mediaChannel.createChannel();
        kParticipant.getMediaChannels().put(this.getId(), mediaChannel);
        log.info("dispatcherMap {}", dispatcherMap);
        return mediaChannel;
    }

    public void initToReady() {
        this.createPipeline();
        this.dispatcher();
        log.info("delivery kms ready");
    }

    public void createPipeline() {
        log.info("SESSION {}: Creating delivery MediaPipeline,kmsIp ({} >> {}),master kms {}", sessionId, kms.getIp(), kms.getId(), kms.getIp());
        try {
            kms.getKurentoClient().createMediaPipeline(new Continuation<MediaPipeline>() {
                @Override
                public void onSuccess(MediaPipeline result) throws Exception {
                    pipeline = result;
                    pipelineLatch.countDown();
                    state = DeliveryKmsStateEnum.CONNECTED;
                    pipeline.setName(MessageFormat.format("delivery_pipeline_{0}_{1}", kms.getId(), sessionId));
                    log.info("SESSION {}: Created MediaPipeline {} in kmsId {}", sessionId, result.getId(), kms.getId());
                }

                @Override
                public void onError(Throwable cause) throws Exception {
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

        pipeline.addErrorListener(new EventListener<ErrorEvent>() {
            @Override
            public void onEvent(ErrorEvent event) {
                String desc = event.getType() + ": " + event.getDescription() + "(errCode=" + event.getErrorCode()
                        + ")";
                log.warn("SESSION {}: Pipeline error encountered: {}", sessionId, desc);
                kurentoSessionHandler.onPipelineError(sessionId, session.getParticipants(), desc);
            }
        });

        log.info("delivery kms create success");
    }
}
