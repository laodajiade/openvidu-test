package io.openvidu.server.kurento.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.layout.LayoutInitHandler;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.utils.SafeSleep;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.kurento.client.EventListener;
import org.kurento.client.*;
import org.kurento.jsonrpc.message.Request;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class CompositeService {

    private final KurentoSession session;
    private MediaPipeline pipeline;

    private Composite composite;
    private final Object compositeCreateLock = new Object();
    private final Object compositeReleaseLock = new Object();

    private HubPort hubPortOut = null;
    private ListenerSubscription hubPortOutSubscription = null;


    private final ThreadPoolExecutor compositeThreadPoolExes;

    private boolean existSharing;

    private final String mixStreamId;

    private String shareStreamId;

    @Getter
    private JsonArray layoutCoordinates = new JsonArray();

    @Getter
    private LayoutModeEnum layoutMode = LayoutModeEnum.ONE;

    private List<CompositeObject> sourcesPublisher = new ArrayList<>();


    public CompositeService(Session session) {
        this.session = (KurentoSession) session;
        this.mixStreamId = session.getSessionId() + "_" + RandomStringUtils.randomAlphabetic(6).toUpperCase() + "_" + "MIX";
        compositeThreadPoolExes = new ThreadPoolExecutor(0, 1, 10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1), new ThreadFactoryBuilder().setNameFormat("composite-thread-" + session.getSessionId() + "-%d")
                .setDaemon(true).build(), new ThreadPoolExecutor.DiscardPolicy());
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
                session.setConferenceMode(ConferenceModeEnum.MCU);
                conferenceLayoutChangedNotify(ProtocolElements.CONFERENCE_MODE_CHANGED_NOTIFY_METHOD);
                asyncUpdateComposite();
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
        hubPortOut = new HubPort.Builder(getComposite()).build();
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

    public Composite getComposite() {
        return composite;
    }

    public boolean isExistSharing() {
        return existSharing;
    }

    public void setExistSharing(boolean existSharing) {
        this.existSharing = existSharing;
    }

    public String getMixStreamId() {
        return mixStreamId;
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


    public void asyncUpdateComposite() {
        compositeThreadPoolExes.execute(this::updateComposite);
    }

    private void updateComposite() {
        log.info("222222222222222222222222222222");
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

            conferenceLayoutChangedNotify(ProtocolElements.CONFERENCE_LAYOUT_CHANGED_NOTIFY);

        } catch (Exception e) {
            log.error("getSipCompositeElements error", e);
        }
    }

    /**
     * 等分布局
     */
    private void normalLayout() {
        List<Participant> parts = session.getParticipants().stream()
                .filter(p -> p.getOrder() < session.getPresetInfo().getSfuPublisherThreshold())
                .sorted(Comparator.comparing(Participant::getOrder))
                .collect(Collectors.toList());

        int mcuNum = 0;
        List<CompositeObject> source = new ArrayList<>(parts.size());
        for (Participant part : parts) {
            mcuNum = getCompositeElements(part, source, StreamType.MAJOR, mcuNum);
        }
        log.info("normal MCU composite number:{} and composite hub port ids:{}", mcuNum, source.toString());
        if (mcuNum > 0) {
            try {
                session.getKms().getKurentoClient().sendJsonRpcRequest(composeLayoutRequest(session.getPipeline().getId(),
                        session.getSessionId(), source, LayoutModeEnum.getLayoutMode(mcuNum)));
                SafeSleep.sleepMilliSeconds(300);
            } catch (Exception e) {
                log.error("Send Sip Composite Layout Exception:\n", e);
            }
        }

        this.sourcesPublisher = source;
    }

    private int getCompositeElements(Participant participant, List<CompositeObject> source, StreamType streamType, int mcuNum) {
        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        PublisherEndpoint publisher = kurentoParticipant.getPublisher(streamType);

        if (publisher == null && streamType == StreamType.MINOR) {
            streamType = StreamType.MAJOR;
            publisher = kurentoParticipant.getPublisher(streamType);
        }

        source.add(new CompositeObject(kurentoParticipant, streamType, publisher));
        return getCompositeElements(publisher, mcuNum);
    }

    /**
     *
     */
    private int getCompositeElements(PublisherEndpoint publisher, int mcuNum) {
        HubPort hubPort;
        if (publisher != null) {
            if (Objects.isNull(hubPort = publisher.getSipCompositeHubPort())) {
                hubPort = publisher.createSipCompositeHubPort(this.composite);
            }
            publisher.getEndpoint().connect(hubPort);
        }
        //publisher.internalSinkConnect(publisher.getEndpoint(), hubPort);
        return ++mcuNum;
    }

    private Request<JsonObject> composeLayoutRequest(String pipelineId, String sessionId, List<CompositeObject> objects, LayoutModeEnum layoutMode) {
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
                CompositeObject compositeObject = objects.get(index.get());
                JsonObject elementsLayout = coordinates.getAsJsonObject().deepCopy();
                PublisherEndpoint publisherEndpoint = compositeObject.endpoint;
                if (publisherEndpoint != null) {
                    elementsLayout.addProperty("streamId", publisherEndpoint.getStreamId());
                    elementsLayout.addProperty("object", publisherEndpoint.getSipCompositeHubPort().getId());
                }
                elementsLayout.addProperty("onlineStatus", "online");
                elementsLayout.addProperty("onlineStatus", "online");
                elementsLayout.addProperty("order", compositeObject.order);
                elementsLayout.addProperty("uuid", compositeObject.uuid);
                elementsLayout.addProperty("username", compositeObject.username);
                elementsLayout.addProperty("streamType", compositeObject.streamType.name());
                elementsLayout.addProperty("onlineStatus", "online");
                elementsLayout.addProperty("hasVideo", publisherEndpoint != null);
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
        this.layoutMode = layoutMode;
        setLayoutCoordinates(layoutInfos);
        return kmsRequest;
    }

    private void setLayoutCoordinates(JsonArray layoutInfos) {
        JsonArray jsonElements = layoutInfos.deepCopy();
        for (JsonElement jsonElement : jsonElements) {
            JsonObject jo = jsonElement.getAsJsonObject();
            jo.remove("object");
        }
        this.layoutCoordinates = jsonElements;
    }

    private void conferenceLayoutChangedNotify(String method) {
        JsonObject params = new JsonObject();
        params.addProperty("roomId", session.getSessionId());
        params.addProperty("conferenceMode", session.getConferenceMode().name());
        params.addProperty("timestamp", System.currentTimeMillis());

        if (session.getConferenceMode() == ConferenceModeEnum.MCU) {
            JsonObject layoutInfoObj = new JsonObject();
            layoutInfoObj.addProperty("mode", layoutMode.getMode());
            layoutInfoObj.add("linkedCoordinates", session.getCompositeService().getLayoutCoordinates());
            params.add("layoutInfo", layoutInfoObj);
        }

        params.add(ProtocolElements.JOINROOM_MIXFLOWS_PARAM, session.getCompositeService().getMixFlowArr());
        session.notifyClient(session.getParticipants(), method, params);
    }


    public JsonArray getMixFlowArr() {
        JsonArray mixFlowsArr = new JsonArray(1);
        if (!StringUtils.isEmpty(this.getMixStreamId())) {
            JsonObject mixJsonObj = new JsonObject();
            mixJsonObj.addProperty(ProtocolElements.JOINROOM_MIXFLOWS_STREAMID_PARAM,
                    this.getMixStreamId());
            mixJsonObj.addProperty(ProtocolElements.JOINROOM_MIXFLOWS_STREAMMODE_PARAM, StreamModeEnum.MIX_MAJOR_AND_SHARING.name());
            mixFlowsArr.add(mixJsonObj);

//            if (!StringUtils.isEmpty(kurentoSession.compositeService.getShareStreamId())) {
//                JsonObject shareJsonObj = new JsonObject();
//                shareJsonObj.addProperty(ProtocolElements.JOINROOM_MIXFLOWS_STREAMID_PARAM,
//                        kurentoSession.compositeService.getShareStreamId());
//                shareJsonObj.addProperty(ProtocolElements.JOINROOM_MIXFLOWS_STREAMMODE_PARAM, StreamModeEnum.SFU_SHARING.name());
//                mixFlowsArr.add(shareJsonObj);
//            }
        }
        return mixFlowsArr;
    }

    private static class CompositeObject {
        String uuid;
        String username;
        int order;
        StreamType streamType;
        PublisherEndpoint endpoint;

        public CompositeObject(Participant participant, StreamType streamType, PublisherEndpoint endpoint) {
            this.uuid = participant.getUuid();
            this.username = participant.getUsername();
            this.order = participant.getOrder();
            this.streamType = streamType;
            this.endpoint = endpoint;
        }

        @Override
        public String toString() {
            return "CompositeObject{" +
                    "uuid='" + uuid + '\'' +
                    ", username='" + username + '\'' +
                    ", streamType=" + streamType +
                    ", endpoint=" + (endpoint == null ? "null" : endpoint.getStreamId()) +
                    '}';
        }
    }
}
