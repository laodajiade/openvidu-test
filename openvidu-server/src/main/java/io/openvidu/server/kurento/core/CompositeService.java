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

    private LayoutModeTypeEnum layoutModeType = LayoutModeTypeEnum.NORMAL;

    private List<CompositeObjectWrapper> sourcesPublisher = new ArrayList<>();


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
        hubPortOut = new HubPort.Builder(composite).build();
        this.hubPortOut.setMinOutputBitrate(2000000);
        this.hubPortOut.setMaxOutputBitrate(2000000);
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
        log.info("do updateComposite {}", session.getSessionId());
        SafeSleep.sleepMilliSeconds(200);
        Set<Participant> participants = session.getParticipants();
        if (participants.isEmpty()) {
            log.warn("participants is empty");
            return;
        }

        try {
            if (session.getSharingPart().isPresent() || session.getSpeakerPart().isPresent()) {
                rostrumLayout();
            } else {
                layoutModeType = LayoutModeTypeEnum.NORMAL;
                normalLayout();
            }

            conferenceLayoutChangedNotify(ProtocolElements.CONFERENCE_LAYOUT_CHANGED_NOTIFY);
        } catch (Exception e) {
            log.error("getSipCompositeElements error", e);
        }
    }


    private void rostrumLayout() {
        Participant moderatorPart = session.getModeratorPart();
        if (moderatorPart != null && moderatorPart.getDeviceModel().equals("T200")) {
            layoutModeType = LayoutModeTypeEnum.ROSTRUM_T200;
        } else {
            layoutModeType = LayoutModeTypeEnum.ROSTRUM;
        }

        List<Participant> parts = session.getParticipants().stream()
                .filter(p -> p.getOrder() < session.getPresetInfo().getSfuPublisherThreshold())
                .sorted(Comparator.comparing(Participant::getOrder))
                .collect(Collectors.toList());

        int mcuNum = 0;
        List<CompositeObjectWrapper> source = new ArrayList<>();

        if (layoutModeType == LayoutModeTypeEnum.ROSTRUM_T200) {
            mcuNum = getT200RostrumElement(parts, mcuNum, source);
        } else {
            mcuNum = getRostrumElement(parts, mcuNum, source);
        }

        log.info("rostrum MCU composite number:{} and composite hub port ids:{}", mcuNum, source.toString());
        if (mcuNum > 0) {
            try {
                session.getKms().getKurentoClient().sendJsonRpcRequest(composeLayoutRequest(session.getPipeline().getId(),
                        session.getSessionId(), source, LayoutModeEnum.getLayoutMode(mcuNum)));
                SafeSleep.sleepMilliSeconds(300);
            } catch (Exception e) {
                log.error("Send Composite Layout Exception:", e);
            }
        }
    }


    private int getT200RostrumElement(List<Participant> parts, int mcuNum, List<CompositeObjectWrapper> source) {
        Participant speaker = session.getSpeakerPart().orElse(null);
        Participant sharing = session.getSharingPart().orElse(null);

        if (speaker != null && sharing != null) {
            mcuNum = getCompositeElements(sharing, source, StreamType.SHARING, mcuNum);
            mcuNum = getCompositeElements(speaker, source, StreamType.MAJOR, mcuNum);
            this.layoutModeType = LayoutModeTypeEnum.ROSTRUM_T200_TWO;
        } else if (speaker != null) {
            mcuNum = getCompositeElements(speaker, source, StreamType.MAJOR, mcuNum);
        } else if (sharing != null) {
            mcuNum = getCompositeElements(sharing, source, StreamType.SHARING, mcuNum);
        } else {
            log.error("speaker and sharing part not found");
            throw new IllegalStateException("speaker and sharing part not found");
        }

        int otherPartSize = 0;
        int i = 0;
        while (otherPartSize < 5) {
            Participant part = parts.get(i++);
            if (speaker != null && part.getUuid().equals(speaker.getUuid())) {
                continue;
            }
            mcuNum = getCompositeElements(part, source, StreamType.MAJOR, mcuNum);
            otherPartSize++;
        }
        return mcuNum;
    }

    private int getRostrumElement(List<Participant> parts, int mcuNum, List<CompositeObjectWrapper> source) {
        Participant speaker = session.getSpeakerPart().orElse(null);
        Participant sharing = session.getSharingPart().orElse(null);
        if (speaker != null) {
            mcuNum = getCompositeElements(speaker, source, StreamType.MAJOR, mcuNum);
        } else if (sharing != null) {
            mcuNum = getCompositeElements(sharing, source, StreamType.SHARING, mcuNum);
        } else {
            log.error("speaker and sharing part not found");
            throw new IllegalStateException("speaker and sharing part not found");
        }

        int otherPartSize = 0;
        int i = 0;
        while (otherPartSize < 3) {
            Participant part = parts.get(i++);
            if (speaker != null && part.getUuid().equals(speaker.getUuid())) {
                continue;
            }
            mcuNum = getCompositeElements(part, source, StreamType.MAJOR, mcuNum);
            otherPartSize++;
        }
        return mcuNum;
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
        List<CompositeObjectWrapper> source = new ArrayList<>(parts.size());
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

    private int getCompositeElements(Participant participant, List<CompositeObjectWrapper> source, StreamType streamType, int mcuNum) {
        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        PublisherEndpoint publisher = kurentoParticipant.getPublisher(streamType);

        if (publisher == null && streamType == StreamType.MINOR) {
            streamType = StreamType.MAJOR;
            publisher = kurentoParticipant.getPublisher(streamType);
        }

        source.add(new CompositeObjectWrapper(kurentoParticipant, streamType, publisher));
        return getCompositeElements(publisher, mcuNum);
    }

    /**
     *
     */
    private int getCompositeElements(PublisherEndpoint publisher, int mcuNum) {
        HubPort hubPort;
        if (publisher != null) {
            if (Objects.isNull(hubPort = publisher.getMajorShareHubPort())) {
                hubPort = publisher.createMajorShareHubPort(this.composite);
            }
            publisher.getEndpoint().connect(hubPort);
        }
        //publisher.internalSinkConnect(publisher.getEndpoint(), hubPort);
        return ++mcuNum;
    }

    private Request<JsonObject> composeLayoutRequest(String pipelineId, String sessionId, List<CompositeObjectWrapper> objects, LayoutModeEnum layoutMode) {
        Request<JsonObject> kmsRequest = new Request<>();
        JsonObject params = new JsonObject();
        params.addProperty("object", pipelineId);
        params.addProperty("operation", "setLayout");
        params.addProperty("sessionId", sessionId);

        // construct composite layout info
        JsonArray layoutInfos = new JsonArray(3);
        JsonArray layoutCoordinates = LayoutInitHandler.getLayoutByMode(layoutModeType, layoutMode);

        AtomicInteger index = new AtomicInteger(0);
        layoutCoordinates.forEach(coordinates -> {
            if (index.get() < layoutMode.getMode()) {
                CompositeObjectWrapper compositeObject = objects.get(index.get());
                JsonObject elementsLayout = coordinates.getAsJsonObject().deepCopy();
                PublisherEndpoint publisherEndpoint = compositeObject.endpoint;
                if (publisherEndpoint != null) {
                    elementsLayout.addProperty("streamId", publisherEndpoint.getStreamId());
                    elementsLayout.addProperty("object", publisherEndpoint.getMajorShareHubPort().getId());
                } else {
                    return;
                }
                elementsLayout.addProperty("onlineStatus", "online");
                elementsLayout.addProperty("onlineStatus", "online");
                elementsLayout.addProperty("order", compositeObject.order);
                elementsLayout.addProperty("uuid", compositeObject.uuid);
                elementsLayout.addProperty("username", compositeObject.username);
                elementsLayout.addProperty("streamType", compositeObject.streamType.name());
                elementsLayout.addProperty("onlineStatus", "online");
                elementsLayout.addProperty("hasVideo", true);
                elementsLayout.addProperty("streaming", publisherEndpoint != null);
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
            mixJsonObj.addProperty(ProtocolElements.JOINROOM_MIXFLOWS_STREAMMODE_PARAM, StreamModeEnum.MIX_MAJOR.name());
            mixFlowsArr.add(mixJsonObj);
        }
        return mixFlowsArr;
    }

    private static class CompositeObjectWrapper {
        String uuid;
        String username;
        int order;
        StreamType streamType;
        PublisherEndpoint endpoint;

        public CompositeObjectWrapper(Participant participant, StreamType streamType, PublisherEndpoint endpoint) {
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
