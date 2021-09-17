package io.openvidu.server.kurento.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.layout.LayoutInitHandler;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.kurento.endpoint.SubscriberEndpoint;
import io.openvidu.server.service.SessionEventRecord;
import io.openvidu.server.utils.SafeSleep;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.kurento.client.*;
import org.kurento.jsonrpc.message.Request;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class CompositeService {

    private final KurentoSession session;
    @Getter
    private MediaPipeline pipeline;
    @Getter
    private Composite composite;
    private final Object compositeCreateLock = new Object();
    private final Object compositeReleaseLock = new Object();

    private HubPort hubPortOut = null;
    private ListenerSubscription hubPortOutSubscription = null;

    private final ThreadPoolExecutor compositeThreadPoolExes;

    private final String mixStreamId;


    @Getter
    private JsonArray layoutCoordinates = new JsonArray();

    @Getter
    private LayoutModeEnum layoutMode = LayoutModeEnum.ONE;

    private LayoutModeTypeEnum layoutModeType = LayoutModeTypeEnum.NORMAL;


    private LayoutModeEnum lastLayoutMode = null;

    private LayoutModeTypeEnum lastLayoutModeType = null;

    private List<CompositeObjectWrapper> sourcesPublisher = new ArrayList<>();

    /**
     * 记录每个拉流ep连接的对象
     * key:subscribeId  value=publisher.hubPort
     */
    private final Map<String, HubPort> subFromHubPorts = new ConcurrentHashMap<>();


    public CompositeService(Session session) {
        this.session = (KurentoSession) session;
        this.mixStreamId = session.getSessionId() + CommonConstants.MIX_STREAM_ID_TRAIT + RandomStringUtils.randomAlphabetic(6).toUpperCase();
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
                SessionEventRecord.startMcu(session, composite, hubPortOut);
            }
            composite.setName(session.getSessionId());
        }


    }

    void closeComposite() {
        releaseHubPortOut();
        synchronized (compositeReleaseLock) {
            if (Objects.isNull(composite)) {
                log.warn("MCU composite already released.");
                return;
            }
            SessionEventRecord.endMcu(session);
            composite.release();
            composite = null;
            log.warn("Release MCU composite");
        }
    }

    private void createHubPortOut() {
        hubPortOut = new HubPort.Builder(composite).build();
        this.hubPortOut.setMinOutputBitrate(1000000);
        this.hubPortOut.setMaxOutputBitrate(2000000);
        hubPortOutSubscription = registerElemErrListener(hubPortOut);
        this.hubPortOut.addTag("debug_name", this.session.getSessionId() + "_mix_hubPort_" + this.session.getRuid().substring(session.getRuid().length() - 6));
        log.info("Sub EP create hubPortOut. {}", this.hubPortOut.getName());
        SessionEventRecord.other(session, "createHubPortOut", " hubPortOutId:" + this.hubPortOut.getId());
    }

    public HubPort getHubPortOut() {
        return this.hubPortOut;
    }

    private void releaseHubPortOut() {
        log.info("Release MajorShareHubPortOut");
        unregisterErrorListeners(hubPortOut, hubPortOutSubscription);
        if (!Objects.isNull(hubPortOut)) {
            SessionEventRecord.other(session, "releaseHubPortOut", " hubPortOutId:" + this.hubPortOut.getId());
            releaseElement(hubPortOut);
        }
    }

    private void releaseElement(final MediaElement element) {
        final String eid = element.getId();
        try {
            element.release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) {
                    log.debug("Released successfully media element #{} ",
                            eid);
                }

                @Override
                public void onError(Throwable cause) {
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
        return element.addErrorListener(event -> {
            // owner.sendMediaError(event);
            log.warn("ListenerSubscription error.{}", event.getDescription());
        });
    }

    protected void unregisterElementErrListener(MediaElement element, final ListenerSubscription subscription) {
        if (element == null || subscription == null) {
            return;
        }
        element.removeErrorListener(subscription);
    }

    public String getMixStreamId() {
        return mixStreamId;
    }

    public void asyncUpdateComposite() {
        compositeThreadPoolExes.execute(this::updateComposite);
    }

    private void updateComposite() {
        SafeSleep.sleepMilliSeconds(200);
        log.info("MCU updateComposite {}", session.getSessionId());
        if (session.isClosed() || session.isClosing()) {
            log.info("session is closed or is closing,break MCU updateComposite");
            return;
        }
        Set<Participant> participants = session.getParticipants();
        if (participants.isEmpty()) {
            log.warn("MCU updateComposite but participants is empty");
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
                        List<CompositeObjectWrapper> oldPoint = this.sourcesPublisher;
                        this.sourcesPublisher = newPoint;
                        releaseCompositeObjectWrapper(oldPoint, newPoint);
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
            if (part.getTerminalType() == TerminalTypeEnum.HDC) {
                getCompositeElements(part, source, StreamType.MINOR);
            } else {
                getCompositeElements(part, source, StreamType.MAJOR);
            }
            otherPartSize++;
        }
    }

    private void getRostrumElement(List<Participant> parts, List<CompositeObjectWrapper> source) {
        Participant speaker = session.getSpeakerPart().orElse(null);
        Participant sharing = session.getSharingPart().orElse(null);
        int otherPartSize = 0;
        if (sharing != null) {
            getCompositeElements(sharing, source, StreamType.SHARING);
            otherPartSize++;
        }
        if (speaker != null) {
            getCompositeElements(speaker, source, StreamType.MAJOR);
            otherPartSize++;
        }
        if (otherPartSize == 0) {
            log.error("speaker and sharing part not found");
            throw new IllegalStateException("speaker and sharing part not found");
        }

        //当有共享，无发言时，大画面显示共享，其他小画面按照参会者列表顺序排列。共享人顺序变为order1.
        if (parts.size() > 1 && sharing != null && speaker == null) {
            if (parts.get(0).getRole() == OpenViduRole.MODERATOR && sharing.getRole() != OpenViduRole.MODERATOR) {
                parts.remove(sharing);
                parts.add(1, sharing);
            } else if (parts.get(0).getRole() != OpenViduRole.MODERATOR) {
                parts.remove(sharing);
                parts.add(0, sharing);
            }
        }

        int i = 0;
        while (otherPartSize < 4 && i < parts.size()) {
            Participant part = parts.get(i++);
            if (speaker != null && part.getUuid().equals(speaker.getUuid())) {
                continue;
            }
            if (part.getTerminalType() == TerminalTypeEnum.HDC) {
                getCompositeElements(part, source, StreamType.MINOR);
            } else {
                getCompositeElements(part, source, StreamType.MAJOR);
            }
            otherPartSize++;
        }
    }

    /**
     * 等分布局
     */
    private List<CompositeObjectWrapper> normalLayout() {
        List<Participant> parts = session.getParticipants().stream()
                .filter(p -> p.getOrder() < session.getPresetInfo().getSfuPublisherThreshold())
                .sorted(Comparator.comparing(Participant::getOrder))
                .collect(Collectors.toList());

        List<CompositeObjectWrapper> source = new ArrayList<>(parts.size());
        StreamType priorityStreamType = parts.size() <= 4 ? StreamType.MAJOR : StreamType.MINOR;
        for (Participant part : parts) {
            if (part.getTerminalType() == TerminalTypeEnum.HDC || part.getTerminalType() == TerminalTypeEnum.S) {
                getCompositeElements(part, source, priorityStreamType);
            } else {
                getCompositeElements(part, source, StreamType.MAJOR);
            }
        }


        log.info("normal MCU composite number:{} and composite hub port ids:{}", source.size(), source.toString());
        return source;
    }

    private void getCompositeElements(Participant participant, List<CompositeObjectWrapper> source, StreamType streamType) {
        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        PublisherEndpoint publisher = kurentoParticipant.getPublisher(streamType);
        if (publisher == null) {
            log.info("{} {}`s publisher is null, create it", participant.getUuid(), streamType);
            publisher = kurentoParticipant.createPublisher(streamType);
            publisher.setCompositeService(this);
//            publisher.setPassThru(new PassThrough.Builder(this.session.getPipeline()).build());
            log.info("{} {} publisher create {}", participant.getUuid(), streamType, publisher.getStreamId());
        }
        CompositeObjectWrapper compositeObjectWrapper = new CompositeObjectWrapper(kurentoParticipant, streamType, publisher);
        if (participant.getTerminalType() != TerminalTypeEnum.S) { //SIP在创建publisher时已经连接上了，整个生命周期都不释放。所以这里没必要进行混流
            compositeObjectWrapper.isStreaming = getCompositeElements(publisher);
        } else {
            compositeObjectWrapper.isStreaming = publisher.getEndpoint() != null;
        }
        source.add(compositeObjectWrapper);
    }

    /**
     *
     */
    private boolean getCompositeElements(PublisherEndpoint publisher) {
        HubPort pubHubPort;
        if (publisher != null && publisher.getEndpoint() != null) {
            if (!publisherIsConnected(publisher.getStreamId())) {
                if (Objects.isNull(pubHubPort = publisher.getPubHubPort())) {
                    pubHubPort = publisher.createMajorShareHubPort(this.composite);
                }
                publisher.getEndpoint().connect(pubHubPort);

                //如果是从墙下MCU上墙，则修改hubPort对象
                if (publisher.getStreamType() == StreamType.MAJOR || publisher.getStreamType() == StreamType.MINOR) {
                    SubscriberEndpoint mixSubscriber = publisher.getOwner().getMixSubscriber();
                    if (mixSubscriber != null) {
                        if (mixSubscriber.getMixHubPort() == null) {
                            log.info("new videoHubPort {} audioHubPort {}", hubPortOut.getName(), pubHubPort.getName());
                            Connect.connectVideoHubAndAudioHub(hubPortOut, pubHubPort, mixSubscriber.getEndpoint(), mixSubscriber.getEndpointName());
                            mixSubscriber.setMixHubPort(hubPortOut);
                            mixSubscriber.setPubHubPort(pubHubPort);
                        } else if (mixSubscriber.getPubHubPort() == null) {
                            log.info("change audioHubPort {} -> {}", hubPortOut.getName(), pubHubPort.getName());
                            Connect.disconnect(hubPortOut, mixSubscriber.getEndpoint(), MediaType.AUDIO, mixSubscriber.getEndpointName());
                            Connect.connect(pubHubPort, mixSubscriber.getEndpoint(), MediaType.AUDIO, mixSubscriber.getEndpointName());
                            mixSubscriber.setPubHubPort(pubHubPort);
                        } else if (mixSubscriber.getPubHubPort() != null && !mixSubscriber.getPubHubPort().getId().equals(pubHubPort.getId())) {
                            log.info("change pubHubPort {} -> {}", mixSubscriber.getPubHubPort().getName(), pubHubPort.getName());
                            Connect.connect(pubHubPort, mixSubscriber.getEndpoint(), MediaType.AUDIO, mixSubscriber.getEndpointName());
                            mixSubscriber.setPubHubPort(pubHubPort);
                        }
                    }
                }
                return true;
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean publisherIsConnected(String streamId) {
        for (CompositeObjectWrapper compositeObjectWrapper : sourcesPublisher) {
            if (compositeObjectWrapper.streamId.equals(streamId) && compositeObjectWrapper.isStreaming) {
                return true;
            }
        }
        return false;
    }

    private Request<JsonObject> composeLayoutRequest(String pipelineId, String sessionId, List<CompositeObjectWrapper> objects, LayoutModeEnum layoutMode) {
        Request<JsonObject> kmsRequest = new Request<>();
        JsonObject params = new JsonObject();
        params.addProperty("object", pipelineId);
        params.addProperty("operation", "setLayout");
        params.addProperty("sessionId", sessionId);

        // construct composite layout info
        JsonArray layoutInfos = new JsonArray(layoutMode.getMode());
        JsonArray showLayout = new JsonArray(layoutMode.getMode());
        JsonArray layoutCoordinates = LayoutInitHandler.getLayoutByMode(layoutModeType, layoutMode, true);

        AtomicInteger index = new AtomicInteger(0);
        layoutCoordinates.forEach(coordinates -> {
            JsonObject elementsLayout = coordinates.getAsJsonObject().deepCopy();
            if (index.get() < layoutMode.getMode()) {
                CompositeObjectWrapper compositeObject = objects.get(index.get());
                index.incrementAndGet();
                PublisherEndpoint publisherEndpoint = compositeObject.endpoint;
                if (publisherEndpoint != null && compositeObject.isStreaming) {
                    elementsLayout.addProperty("streamId", publisherEndpoint.getStreamId());
                    elementsLayout.addProperty("object", publisherEndpoint.getPubHubPort().getId());
                } else {
                    log.info("layoutCoordinates compositeObject.uuid {} streaming is {} ", compositeObject.uuid, compositeObject.isStreaming);
                    return;
                }
                elementsLayout.addProperty("order", compositeObject.order);
                elementsLayout.addProperty("uuid", compositeObject.uuid);
                elementsLayout.addProperty("username", compositeObject.username);
                elementsLayout.addProperty("streamType", compositeObject.streamType.name());
                elementsLayout.addProperty("connectionId", "connectionId");
                elementsLayout.addProperty("onlineStatus", "online");
                elementsLayout.addProperty("hasVideo", true);
                elementsLayout.addProperty("streaming", publisherEndpoint.isStreaming());
                layoutInfos.add(elementsLayout);
            } else {
                // 补足无画面布局
                elementsLayout.addProperty("uuid", "");
            }
            showLayout.add(elementsLayout);
        });

        JsonObject operationParams = new JsonObject();
        operationParams.add("layoutInfo", layoutInfos);
        params.add("operationParams", operationParams);
        kmsRequest.setMethod("invoke");
        kmsRequest.setParams(params);
        log.info("send mcu composite setLayout params:{}", params);
        this.layoutMode = LayoutInitHandler.ceil(layoutModeType, layoutMode);
        setLayoutCoordinates(showLayout);
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
     * @param streamChange 是否对比streamId的变化,客户端不需要知道流变化
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

    /**
     * SIP特殊处理逻辑
     */
    public void connectSip(PublisherEndpoint publisher) {
        HubPort pubHubPort;
        if (Objects.isNull(pubHubPort = publisher.getPubHubPort())) {
            pubHubPort = publisher.createMajorShareHubPort(this.composite);
        }
        publisher.getEndpoint().connect(pubHubPort);
        Connect.connectVideoHubAndAudioHub(this.hubPortOut, pubHubPort, publisher.getEndpoint(), publisher.getEndpointName());

    }

    public void sinkConnect(SubscriberEndpoint subscriberEndpoint) {
        Participant participant = subscriberEndpoint.getOwner();
        for (CompositeObjectWrapper compositeObjectWrapper : this.sourcesPublisher) {
            if (compositeObjectWrapper.uuid.equals(participant.getUuid()) && compositeObjectWrapper.isStreaming) {
                log.info("sink connect self publisher {} {}", participant.getUuid(), compositeObjectWrapper.streamId);
                Connect.connectVideoHubAndAudioHub(hubPortOut, compositeObjectWrapper.endpoint.getPubHubPort(), subscriberEndpoint.getEndpoint(), subscriberEndpoint.getEndpointName());
                subscriberEndpoint.setMixHubPort(hubPortOut);
                subscriberEndpoint.setPubHubPort(compositeObjectWrapper.endpoint.getPubHubPort());
                return;
            }
        }
        connectHubPortOut(subscriberEndpoint);
    }

    public void connectHubPortOut(SubscriberEndpoint subscriberEndpoint) {
        Connect.connect(this.getHubPortOut(), subscriberEndpoint);
        subscriberEndpoint.setPubHubPort(null);
    }


    /**
     * 释放掉已经不需要的hubPort
     */
    private void releaseCompositeObjectWrapper(List<CompositeObjectWrapper> oldPoint, List<CompositeObjectWrapper> newPoint) {
        for (CompositeObjectWrapper source : oldPoint) {
            if (newPoint.stream().anyMatch(target -> target.uuid.equals(source.uuid) && target.streamId.equals(source.streamId))) {
                continue;
            }

            if (source.streamType == StreamType.SHARING) {
                log.info("uuid {} end sharing,reconnect hubPort", source.uuid);
                source.endpoint.releaseMajorShareHubPort();
            }

            // 下墙或离会了,下墙后连接到hubPortOut
            if (newPoint.stream().anyMatch(target -> target.uuid.equals(source.uuid) && target.streamType != StreamType.SHARING)) {
                source.endpoint.releaseMajorShareHubPort();
                smartReconnect(source);
            }

            //smartReconnect(newPoint, source);


        }
    }

    // 下墙或离会了,如果是下墙需要连接到hubPortOut
    private void smartReconnect(CompositeObjectWrapper source) {
        if (source == null) {
            return;
        }
        SubscriberEndpoint mixSubscriber = source.endpoint.getOwner().getMixSubscriber();
        if (mixSubscriber == null) {
            return;
        }
        log.info("uuid {} down wall or leave,{} reconnect hubPort", source.uuid, mixSubscriber.getEndpointName());
        Connect.connect(hubPortOut, mixSubscriber);
        mixSubscriber.setPubHubPort(null);
    }

    private static class CompositeObjectWrapper {
        String uuid;
        String username;
        int order;
        StreamType streamType;
        String streamId;
        PublisherEndpoint endpoint;
        boolean isStreaming = false;

        public CompositeObjectWrapper(Participant participant, StreamType streamType, PublisherEndpoint endpoint) {
            this.uuid = participant.getUuid();
            this.username = participant.getUsername();
            this.order = participant.getOrder();
            this.streamType = streamType;
            this.endpoint = endpoint;
            if (endpoint != null) {
                this.streamId = endpoint.getStreamId();
            }
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
            return Objects.hash(uuid, username, order, isStreaming, streamType);
        }

        @Override
        public String toString() {
            return "CompositeObject{" +
                    "uuid='" + uuid + '\'' +
                    ", username='" + username + '\'' +
                    ", streamType=" + streamType +
                    ", isStreaming=" + isStreaming +
                    ", endpoint=" + (endpoint == null ? "null" : endpoint.getStreamId()) +
                    '}';
        }
    }

    static class Connect {
        /**
         * SIP特殊处理逻辑
         */
        public void sipConnect(PublisherEndpoint publisher) {

        }

//        private static void connectVideoHubAndAudioHub(final HubPort videoHubPort, final HubPort audioHubPort, final SubscriberEndpoint subscriberEndpoint) {
//            connectVideoHubAndAudioHub(videoHubPort, audioHubPort, subscriberEndpoint.getEndpoint(), subscriberEndpoint.getEndpointName());
//
//            subscriberEndpoint.setMixHubPort(videoHubPort);
//            subscriberEndpoint.setPubHubPort(audioHubPort);
//        }

        private static void connectVideoHubAndAudioHub(final HubPort videoHubPort, final HubPort audioHubPort,
                                                       final MediaElement sink, String sinkEndpointName) {
            disconnect(videoHubPort, sink, MediaType.AUDIO, sinkEndpointName);
            connect(videoHubPort, sink, MediaType.VIDEO, sinkEndpointName);
            connect(audioHubPort, sink, MediaType.AUDIO, sinkEndpointName);
        }


        private static void connect(final HubPort hubPort, final SubscriberEndpoint subscriberEndpoint) {
            hubPort.connect(subscriberEndpoint.getEndpoint(), new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) {
                    log.info("MCU subscribe {}: Elements have been connected (source {} -> sink {})", subscriberEndpoint.getStreamId(),
                            hubPort.getTag("debug_name"), subscriberEndpoint.getEndpoint().getId());
                }

                @Override
                public void onError(Throwable cause) {
                    log.warn("MCU subscribe {}: Failed to connect media elements (source {} -> sink {})", subscriberEndpoint.getStreamId(),
                            hubPort.getTag("debug_name"), subscriberEndpoint.getEndpoint().getId(), cause);
                }
            });
        }


        private static void connect(final HubPort source, final MediaElement sink, final MediaType mediaType, String sinkEndpointName) {
            source.connect(sink, mediaType, new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) {
                    log.info("MCU subscribe {} {}: Elements have been connected (source {} -> sink {})", sinkEndpointName, mediaType.name(),
                            source.getTag("debug_name"), sink.getId());
                }

                @Override
                public void onError(Throwable cause) {
                    log.warn("MCU subscribe {} {}: Failed to connect media elements (source {} -> sink {})", sinkEndpointName, mediaType.name(),
                            source.getTag("debug_name"), sink.getId(), cause);
                }
            });
        }

        //**********************************************************************************

        private static void disconnect(final HubPort source, final MediaElement sink, final MediaType type, String sinkEndPointName) {
            if (type == null) {
                Connect.disconnect(source, sink, sinkEndPointName);
            } else {
                source.disconnect(sink, type, new Continuation<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        log.info("MCU subscribe {}: {} media elements have been disconnected (source {} -> sink {})",
                                sinkEndPointName, type, source.getTag("debug_name"), sink.getId());
                    }

                    @Override
                    public void onError(Throwable cause) {
                        log.info("MCU subscribe {}: Failed to disconnect {} media elements (source {} -> sink {})", sinkEndPointName,
                                type, source.getTag("debug_name"), sink.getId(), cause);
                    }
                });
            }
        }

        private static void disconnect(final MediaElement source, final MediaElement sink, String sinkEndPointName) {
            source.disconnect(sink, new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) {
                    log.debug("MCU subscribe {}: Elements have been disconnected (source {} -> sink {})", sinkEndPointName,
                            source.getTag("debug_name"), sink.getId());
                }

                @Override
                public void onError(Throwable cause) {
                    log.warn("MCU subscribe {}: Failed to disconnect media elements (source {} -> sink {})", sinkEndPointName,
                            source.getTag("debug_name"), sink.getId(), cause);
                }
            });
        }


    }
}
