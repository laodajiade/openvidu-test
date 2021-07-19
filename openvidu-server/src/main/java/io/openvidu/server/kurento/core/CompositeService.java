package io.openvidu.server.kurento.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.layout.LayoutInitHandler;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.utils.SafeSleep;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.EventListener;
import org.kurento.client.*;
import org.kurento.jsonrpc.message.Request;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class CompositeService {

    private KurentoSession session;
    private MediaPipeline pipeline;

    private Composite composite;
    private final Object compositeCreateLock = new Object();
    private final Object compositeReleaseLock = new Object();

    private HubPort hubPortOut = null;
    private ListenerSubscription hubPortOutSubscription = null;


    private boolean existSharing;

    private String mixMajorShareStreamId;
    private String shareStreamId;

    private List<PublisherEndpoint> sources = new ArrayList<>();


    public CompositeService(Session session) {
        this.session = (KurentoSession) session;
    }

    public void createComposite(MediaPipeline pipeline) {
        if (composite == null) {
            synchronized (compositeCreateLock) {
                if (composite != null) {
                    return;
                }

                this.pipeline = pipeline;
                log.info("SESSION {}: Creating Composite", session.getSessionId());
                composite = new Composite.Builder(this.pipeline).build();
                createHubPortOut();
                updateComposite();
            }
            composite.setName(session.getSessionId());
        }


//        majorShareHubPortOut.setName(session.getSessionId() + "_mix_out");
    }

    void closeComposite() {
        releaseHubPortOut();
        synchronized (compositeReleaseLock) {
            if (Objects.isNull(composite)) {
                log.warn("majorShareComposite already released.");
                return;
            }
            composite.release();
            composite = null;
            log.warn("Release MajorShareComposite");
        }
    }

    private void createHubPortOut() {
        hubPortOut = new HubPort.Builder(getMajorShareComposite()).build();
        hubPortOutSubscription = registerElemErrListener(hubPortOut);
        log.info("Sub EP create hubPortOut.");
    }

    public HubPort getHubPortOut() {
        return this.hubPortOut;
    }

    private void releaseHubPortOut() {
        log.info("Release MajorShareHubPortOut");
        unregisterErrorListeners(hubPortOut, hubPortOutSubscription);
        if (!Objects.isNull(hubPortOut)) {
            releaseElement(hubPortOut);
        }
    }

    private void releaseElement(final MediaElement element) {
        final String eid = element.getId();
        try {
            element.release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.debug("Released successfully media element #{} ",
                            eid);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("Could not release media element #{}",
                            eid, cause);
                }
            });
        } catch (Exception e) {
            log.error("Error calling release on elem #{} for {}", eid, e);
        }
    }

    private synchronized void unregisterErrorListeners(HubPort hubPort, ListenerSubscription listenerSubscription) {
        if (!Objects.isNull(hubPort)) {
            unregisterElementErrListener(hubPort, listenerSubscription);
        }
    }

    protected ListenerSubscription registerElemErrListener(MediaElement element) {
        return element.addErrorListener(new EventListener<ErrorEvent>() {
            @Override
            public void onEvent(ErrorEvent event) {
                // owner.sendMediaError(event);
                log.warn("ListenerSubscription error.{}", event.getDescription());
            }
        });
    }

    protected void unregisterElementErrListener(MediaElement element, final ListenerSubscription subscription) {
        if (element == null || subscription == null) {
            return;
        }
        element.removeErrorListener(subscription);
    }

    public Composite getMajorShareComposite() {
        return composite;
    }

    public boolean isExistSharing() {
        return existSharing;
    }

    public void setExistSharing(boolean existSharing) {
        this.existSharing = existSharing;
    }

    public void setMixMajorShareStreamId(String mixMajorShareStreamId) {
        this.mixMajorShareStreamId = mixMajorShareStreamId;
    }

    public String getMixMajorShareStreamId() {
        return mixMajorShareStreamId;
    }

    public String getShareStreamId() {
        return shareStreamId;
    }

    public void setShareStreamId(String shareStreamId) {
        this.shareStreamId = shareStreamId;
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }


    public void updateComposite() {
        log.info("1111111111111111111111111 updateComposite ");
        SafeSleep.sleepMilliSeconds(200);

        Participant moderator = null, sharing = null, speaker = null;
        Set<Participant> participants = session.getParticipants();

        if (participants.isEmpty()) {
            log.warn("participants is empty");
            return;
        }

//        for (Participant participant : participants) {
//            if (OpenViduRole.MODERATOR == participant.getRole()) {
//                moderator = participant;
//            }
//            sharing = session.getSharingPart().orElse(null);
//            speaker = session.getSpeakerPart().orElse(null);
//        }

//        if (kurentoSession.equalsSipCompositeStream(moderator, sharing, speaker)) {
//            log.info("sip composite stream list no change");
//            return;
//        }

        // set composite order
        try {
//            if (Objects.nonNull(sharing)) {
//                log.info("sip found sharing");
//                mcuNum = getCompositeElements(sharing, hubPortIds, mcuNum);
//            }
//            if (Objects.nonNull(speaker)) {
//                log.info("sip found speaker");
//                mcuNum = getCompositeElements(speaker, hubPortIds, mcuNum);
//            }
//            if (Objects.nonNull(moderator)) {
//                log.info("sip found moderator");
//                mcuNum = getCompositeElements(moderator, hubPortIds, mcuNum);
//            }


            normalLayout();


//            log.info("sip MCU composite number:{} and composite hub port ids:{}", mcuNum, hubPortIds.toString());
//            if (mcuNum > 0) {
//                try {
//                    session.getKms().getKurentoClient()
//                            .sendJsonRpcRequest(composeLayoutRequest(session.getPipeline().getId(),
//                                    session.getSessionId(), hubPortIds, LayoutModeEnum.getLayoutMode(mcuNum)));
//                    SafeSleep.sleepMilliSeconds(300);
//                } catch (Exception e) {
//                    log.error("Send Sip Composite Layout Exception:\n", e);
//                }
//            }
        } catch (Exception e) {
            log.error("getSipCompositeElements error", e);
        }
    }

    /**
     * 等分布局
     */
    private void normalLayout() {
        log.info("222222222222222222222222222");
        List<Participant> parts = session.getParticipants().stream()
                .filter(p -> p.getOrder() < session.getPresetInfo().getSfuPublisherThreshold())
                .sorted(Comparator.comparing(Participant::getOrder))
                .collect(Collectors.toList());

        int mcuNum = 0;
        List<PublisherEndpoint> publishers = new ArrayList<>(parts.size());
        for (Participant part : parts) {
            mcuNum = getCompositeElements(part, publishers, mcuNum);
        }
        log.info("33333333333333333333333333333");
        log.info("normal MCU composite number:{} and composite hub port ids:{}", mcuNum, publishers.toString());
        if (mcuNum > 0) {
            try {
                log.info("555555555555555555555555555");
                session.getKms().getKurentoClient().sendJsonRpcRequest(composeLayoutRequest(session.getPipeline().getId(),
                        session.getSessionId(), publishers, LayoutModeEnum.getLayoutMode(mcuNum)));
                SafeSleep.sleepMilliSeconds(300);
                log.info("6666666666666666666666666");
            } catch (Exception e) {
                log.error("Send Sip Composite Layout Exception:\n", e);
            }
        }
        log.info("444444444444444444444444");
    }

    private int getCompositeElements(Participant participant, List<PublisherEndpoint> publishers, int mcuNum) {
        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        PublisherEndpoint publisher = kurentoParticipant.getPublisher(StreamType.MAJOR);
        return getCompositeElements(publisher, publishers, mcuNum);
    }

    private int getCompositeElements(PublisherEndpoint publisher, List<PublisherEndpoint> publishers, int mcuNum) {
        HubPort hubPort;
        if (Objects.isNull(hubPort = publisher.getSipCompositeHubPort())) {
            hubPort = publisher.createSipCompositeHubPort(this.composite);
        }
        publisher.getEndpoint().connect(hubPort);
        //publisher.internalSinkConnect(publisher.getEndpoint(), hubPort);
        publishers.add(publisher);
        return ++mcuNum;
    }

    private Request<JsonObject> composeLayoutRequest(String pipelineId, String sessionId, List<PublisherEndpoint> publishers, LayoutModeEnum layoutMode) {
        Request<JsonObject> kmsRequest = new Request<>();
        JsonObject params = new JsonObject();
        params.addProperty("object", pipelineId);
        params.addProperty("operation", "setLayout");
        params.addProperty("sessionId", sessionId);

        // construct composite layout info
        JsonArray layoutInfos = new JsonArray(3);
        JsonArray layoutCoordinates = LayoutInitHandler.getLayoutByMode(layoutMode);
        AtomicInteger index = new AtomicInteger(0);
        layoutCoordinates.forEach(coordinates -> {
            if (index.get() < layoutMode.getMode()) {

                PublisherEndpoint publisherEndpoint = publishers.get(index.get());
                JsonObject elementsLayout = coordinates.getAsJsonObject().deepCopy();
                elementsLayout.addProperty("connectionId", publisherEndpoint.getStreamId());
                elementsLayout.addProperty("streamType", publisherEndpoint.getStreamType().name());
                elementsLayout.addProperty("object", publisherEndpoint.getSipCompositeHubPort().getId());
                elementsLayout.addProperty("hasVideo", true);
                elementsLayout.addProperty("onlineStatus", "online");
                index.incrementAndGet();
                layoutInfos.add(elementsLayout);
            }
        });

        JsonObject operationParams = new JsonObject();
        operationParams.add("layoutInfo", layoutInfos);
        params.add("operationParams", operationParams);
        kmsRequest.setMethod("invoke");
        kmsRequest.setParams(params);
        log.info("send mcu composite setLayout params:{}", params);

        return kmsRequest;
    }
}
