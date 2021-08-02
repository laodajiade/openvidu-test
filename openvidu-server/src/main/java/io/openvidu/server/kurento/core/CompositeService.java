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


    private LayoutModeEnum lastLayoutMode = null;

    private LayoutModeTypeEnum lastLayoutModeType = null;

    private List<CompositeObjectWrapper> sourcesPublisher = new ArrayList<>();


    public CompositeService(Session session) {
        this.session = (KurentoSession) session;
        this.mixStreamId = session.getSessionId() + "_MIX_" + RandomStringUtils.randomAlphabetic(6).toUpperCase();
        compositeThreadPoolExes = new ThreadPoolExecutor(0, 1, 10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1), new ThreadFactoryBuilder().setNameFormat("composite-thread-" + session.getSessionId() + "-%d")
                .setDaemon(true).build(), new ThreadPoolExecutor.DiscardPolicy());
    }

    public void createComposite() {
        if (composite == null) {
            synchronized (compositeCreateLock) {
                if (composite != null) {
                    return;
                }

                this.pipeline = session.getPipeline();
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
                log.warn("MCU composite already released.");
                return;
            }
            composite.release();
            composite = null;
            log.warn("Release MCU composite");
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
        log.info("MCU updateComposite {}", session.getSessionId());
        SafeSleep.sleepMilliSeconds(200);
        Set<Participant> participants = session.getParticipants();
        if (participants.isEmpty()) {
            log.warn("MCU updateComposite participants is empty");
            return;
        }

        try {
            List<CompositeObjectWrapper> newPoint;
            if (session.getSharingPart().isPresent() || session.getSpeakerPart().isPresent()) {
                newPoint = rostrumLayout();
            } else {
                layoutModeType = LayoutModeTypeEnum.NORMAL;
                newPoint = normalLayout();
            }

            if (isLayoutChange(newPoint, true)) {
                log.info("The layout of {} has changed", session.getSessionId());
                if (newPoint.size() > 0) {
                    try {
                        session.getKms().getKurentoClient().sendJsonRpcRequest(composeLayoutRequest(session.getPipeline().getId(),
                                session.getSessionId(), newPoint, LayoutModeEnum.getLayoutMode(newPoint.size())));
                        if (isLayoutChange(newPoint, false)) {
                            conferenceLayoutChangedNotify(ProtocolElements.CONFERENCE_LAYOUT_CHANGED_NOTIFY);
                        }

                        this.lastLayoutModeType = this.layoutModeType;
                        this.lastLayoutMode = this.layoutMode;
                        this.sourcesPublisher = newPoint;
                    } catch (Exception e) {
                        log.error("Send Composite Layout Exception:", e);
                    }
                }
            } else {
                log.info("The layout of {} has not changed", session.getSessionId());
            }
        } catch (Exception e) {
            log.error("MCU update Composite error", e);
        }
    }


    private List<CompositeObjectWrapper> rostrumLayout() {
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

        List<CompositeObjectWrapper> source = new ArrayList<>();

        if (layoutModeType == LayoutModeTypeEnum.ROSTRUM_T200) {
            getT200RostrumElement(parts, source);
        } else {
            getRostrumElement(parts, source);
        }

        log.info("rostrum MCU composite number:{} and composite hub port ids:{}", source.size(), source.toString());
        return source;
    }


    private void getT200RostrumElement(List<Participant> parts, List<CompositeObjectWrapper> source) {
        Participant speaker = session.getSpeakerPart().orElse(null);
        Participant sharing = session.getSharingPart().orElse(null);

        if (speaker != null && sharing != null) {
            getCompositeElements(sharing, source, StreamType.SHARING);
            getCompositeElements(speaker, source, StreamType.MAJOR);
            this.layoutModeType = LayoutModeTypeEnum.ROSTRUM_T200_TWO;
        } else if (speaker != null) {
            getCompositeElements(speaker, source, StreamType.MAJOR);
        } else if (sharing != null) {
            getCompositeElements(sharing, source, StreamType.SHARING);
        } else {
            log.error("speaker and sharing part not found");
            throw new IllegalStateException("speaker and sharing part not found");
        }

        int otherPartSize = 0;
        int i = 0;
        while (otherPartSize < 5 && i < parts.size()) {
            Participant part = parts.get(i++);
            if (speaker != null && part.getUuid().equals(speaker.getUuid())) {
                continue;
            }
            getCompositeElements(part, source, StreamType.MAJOR);
            otherPartSize++;
        }
    }

    private void getRostrumElement(List<Participant> parts, List<CompositeObjectWrapper> source) {
        Participant speaker = session.getSpeakerPart().orElse(null);
        Participant sharing = session.getSharingPart().orElse(null);
        if (speaker != null) {
            getCompositeElements(speaker, source, StreamType.MAJOR);
        } else if (sharing != null) {
            getCompositeElements(sharing, source, StreamType.SHARING);
        } else {
            log.error("speaker and sharing part not found");
            throw new IllegalStateException("speaker and sharing part not found");
        }

        int otherPartSize = 0;
        int i = 0;
        while (otherPartSize < 3 && i < parts.size()) {
            Participant part = parts.get(i++);
            if (speaker != null && part.getUuid().equals(speaker.getUuid())) {
                continue;
            }
            getCompositeElements(part, source, StreamType.MAJOR);
            otherPartSize++;
        }
    }

    /**
     * 等分布局
     * return 如果布局有变化，返回true，否则返回false
     */
    private List<CompositeObjectWrapper> normalLayout() {
        List<Participant> parts = session.getParticipants().stream()
                .filter(p -> p.getOrder() < session.getPresetInfo().getSfuPublisherThreshold())
                .sorted(Comparator.comparing(Participant::getOrder))
                .collect(Collectors.toList());

        List<CompositeObjectWrapper> source = new ArrayList<>(parts.size());
        for (Participant part : parts) {
            getCompositeElements(part, source, StreamType.MAJOR);
        }


        log.info("normal MCU composite number:{} and composite hub port ids:{}", source.size(), source.toString());
        return source;
    }

    private void getCompositeElements(Participant participant, List<CompositeObjectWrapper> source, StreamType streamType) {
        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        PublisherEndpoint publisher = kurentoParticipant.getPublisher(streamType);
        if (publisher == null) {
            log.info("{} {}`s publisher is null, create it", participant.getUuid(), streamType);
//            publisher = new PublisherEndpoint(true, kurentoParticipant, kurentoParticipant.getUuid(),
//                    kurentoParticipant.getSession().getPipeline(), streamType, this.session.getOpenviduConfig());
            publisher = kurentoParticipant.createPublisher(streamType);
            publisher.setCompositeService(this);
            publisher.setPassThru(new PassThrough.Builder(this.session.getPipeline()).build());
            kurentoParticipant.setPublisher(streamType, publisher);
            log.info("{} {} publisher create {}", participant.getUuid(), streamType, publisher.getStreamId());
        }

        source.add(new CompositeObjectWrapper(kurentoParticipant, streamType, publisher));
        getCompositeElements(publisher);
    }

    /**
     *
     */
    private void getCompositeElements(PublisherEndpoint publisher) {
        HubPort hubPort;
        if (publisher != null) {
            if (Objects.isNull(hubPort = publisher.getMajorShareHubPort())) {
                hubPort = publisher.createMajorShareHubPort(this.composite);
            }
            if (publisher.getEndpoint() != null) {
                publisher.getEndpoint().connect(hubPort);
            }
        }
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
                elementsLayout.addProperty("order", compositeObject.order);
                elementsLayout.addProperty("uuid", compositeObject.uuid);
                elementsLayout.addProperty("username", compositeObject.username);
                elementsLayout.addProperty("streamType", compositeObject.streamType.name());
                //elementsLayout.addProperty("streamType", "streamType");
                elementsLayout.addProperty("connectionId", "connectionId");
                elementsLayout.addProperty("onlineStatus", "online");
                elementsLayout.addProperty("hasVideo", true);
                //elementsLayout.addProperty("streaming", publisherEndpoint != null);
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

    /**
     * 前后布局对比
     *
     * @param streamChange 是否对比streamId的变化
     * @return 有变化返回true
     */
    private boolean isLayoutChange(List<CompositeObjectWrapper> newObjects, boolean streamChange) {
        if (layoutMode != lastLayoutMode || layoutModeType != lastLayoutModeType) {
            return true;
        }
        if (this.sourcesPublisher.size() != newObjects.size()) {
            return true;
        }

        for (int i = 0; i < sourcesPublisher.size(); i++) {
            if (!Objects.equals(sourcesPublisher.get(i), newObjects.get(i))) {
                return true;
            }
            if (streamChange && !Objects.equals(sourcesPublisher.get(i).endpoint.getStreamId(), newObjects.get(i).endpoint.getStreamId())) {
                return true;
            }
        }
        return false;
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompositeObjectWrapper that = (CompositeObjectWrapper) o;
            return order == that.order &&
                    Objects.equals(uuid, that.uuid) &&
                    Objects.equals(username, that.username) &&
                    streamType == that.streamType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, username, order, streamType);
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
