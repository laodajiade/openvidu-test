package io.openvidu.server.kurento.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.redis.RecordingRedisPublisher;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.PassThrough;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RecorderService {

    private final KurentoSession session;

    private final Object createLock = new Object();

    private boolean existSharing;

    @Getter
    private JsonArray layoutCoordinates = new JsonArray();

    @Getter
    private LayoutModeEnum layoutMode = LayoutModeEnum.ONE;

    private LayoutModeTypeEnum layoutModeType = LayoutModeTypeEnum.NORMAL;

    private List<CompositeObjectWrapper> sourcesPublisher = new ArrayList<>();

    private RecordingRedisPublisher recordingRedisPublisher;

    private JsonArray passThruList = new JsonArray();

    public RecorderService(Session session, RecordingRedisPublisher recordingRedisPublisher) {
        this.session = (KurentoSession) session;
        this.recordingRedisPublisher = recordingRedisPublisher;
    }

    public void startRecording() {
        ConferenceRecordingProperties recordingProperties = ConferenceRecordingProperties.builder()
                .project(session.getConference().getProject())
                .roomId(session.getSessionId())
                .ruid(session.getRuid())
                .startTime(session.getStartRecordingTime())
                .updateTime(System.currentTimeMillis())
                .rootPath(session.getOpenviduConfig().getRecordingPath())
                .outputMode(RecordOutputMode.COMPOSED)
                .mediaProfileSpecType(MediaProfileSpecType.valueOf(session.getOpenviduConfig().getMediaProfileSpecType())).build();
        if (this.constructMediaSources(recordingProperties)) {
            // pub start recording task
            recordingRedisPublisher.sendRecordingTask(RecordingOperationEnum.startRecording.buildMqMsg(recordingProperties).toString());
        }
        session.setIsRecording(true);
    }

    public void updateRecording() {
        ConferenceRecordingProperties recordingProperties = ConferenceRecordingProperties.builder()
                .ruid(session.getRuid())
                .updateTime(System.currentTimeMillis())
                .outputMode(RecordOutputMode.COMPOSED).build();
        if (this.constructMediaSources(recordingProperties)) {
            // pub start recording task
            recordingRedisPublisher.sendRecordingTask(RecordingOperationEnum.updateRecording.buildMqMsg(recordingProperties).toString());
        } else {
            log.warn("Not found required participant and do not update the recording.");
        }
    }

    public void stopRecording() {
        // pub stop recording task
        recordingRedisPublisher.sendRecordingTask(RecordingOperationEnum.stopRecording.buildMqMsg(ConferenceRecordingProperties.builder()
                .ruid(session.getRuid()).outputMode(RecordOutputMode.COMPOSED).build()).toString());
        session.setIsRecording(false);
    }


    private boolean constructMediaSources(ConferenceRecordingProperties recordingProperties) {
        Set<Participant> participants = session.getParticipants();
        if (participants.isEmpty()) {
            log.warn("participants is empty");
            return false;
        }

        try {
            if (session.getSharingPart().isPresent() || session.getSpeakerPart().isPresent()) {
                rostrumLayout();
            } else {
                layoutModeType = LayoutModeTypeEnum.NORMAL;
                normalLayout();
            }
            layoutMode = LayoutModeEnum.getLayoutMode(sourcesPublisher.size());

            JsonObject mediaSourceObj = new JsonObject();
            mediaSourceObj.addProperty("kmsLocated", session.getKms().getIp());
            mediaSourceObj.addProperty("mediaPipelineId", session.getPipeline().getId());
            mediaSourceObj.add("passThruList", passThruList);

            JsonArray mediaSources = new JsonArray();
            mediaSources.add(mediaSourceObj);
            recordingProperties.setMediaSources(mediaSources);
            recordingProperties.setLayoutMode(layoutMode.getMode());
            recordingProperties.setLayoutModeTypeEnum(layoutModeType);
        } catch (Exception e) {
            log.error("getSipCompositeElements error", e);
            return false;
        }
        return true;
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


        List<CompositeObjectWrapper> source = new ArrayList<>();

        if (layoutModeType == LayoutModeTypeEnum.ROSTRUM_T200) {
            getT200RostrumElement(parts, source);
        } else {
            getRostrumElement(parts, source);
        }
        this.sourcesPublisher = source;
        log.info("recorder size {}", sourcesPublisher.size());
//        if (sourcesPublisher.size() > 0) {
//            try {
//                session.getKms().getKurentoClient().sendJsonRpcRequest(composeLayoutRequest(session.getPipeline().getId(),
//                        session.getSessionId(), source, LayoutModeEnum.getLayoutMode(mcuNum)));
//                SafeSleep.sleepMilliSeconds(300);
//            } catch (Exception e) {
//                log.error("Send Composite Layout Exception:", e);
//            }
//        }
    }


    private void getT200RostrumElement(List<Participant> parts, List<CompositeObjectWrapper> source) {
        Participant speaker = session.getSpeakerPart().orElse(null);
        Participant sharing = session.getSharingPart().orElse(null);
        JsonArray passThruList = new JsonArray();
        int order = 0;
        if (speaker != null && sharing != null) {
            passThruList.add(constructPartRecordInfo(sharing, source, StreamType.SHARING, order++));
            passThruList.add(constructPartRecordInfo(speaker, source, StreamType.MAJOR, order++));
            this.layoutModeType = LayoutModeTypeEnum.ROSTRUM_T200_TWO;
        } else if (speaker != null) {
            passThruList.add(constructPartRecordInfo(speaker, source, StreamType.MAJOR, order++));
        } else if (sharing != null) {
            passThruList.add(constructPartRecordInfo(sharing, source, StreamType.SHARING, order++));
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
                passThruList.add(constructPartRecordInfo(part, source, StreamType.MINOR, order++));
            } else {
                passThruList.add(constructPartRecordInfo(part, source, StreamType.MAJOR, order++));
            }
            otherPartSize++;
        }

        this.passThruList = passThruList;
    }

    private void getRostrumElement(List<Participant> parts, List<CompositeObjectWrapper> source) {
        Participant speaker = session.getSpeakerPart().orElse(null);
        Participant sharing = session.getSharingPart().orElse(null);
        JsonArray passThruList = new JsonArray();
        int order = 0;
        if (speaker != null) {
            passThruList.add(constructPartRecordInfo(speaker, source, StreamType.MAJOR, order++));
        } else if (sharing != null) {
            passThruList.add(constructPartRecordInfo(sharing, source, StreamType.SHARING, order++));
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
            if (part.getTerminalType() == TerminalTypeEnum.HDC) {
                passThruList.add(constructPartRecordInfo(part, source, StreamType.MINOR, order++));
            } else {
                passThruList.add(constructPartRecordInfo(part, source, StreamType.MAJOR, order++));
            }
            otherPartSize++;
        }
        this.passThruList = passThruList;
    }

    /**
     * 等分布局
     */
    private void normalLayout() {
        List<Participant> parts = session.getParticipants().stream()
                .filter(p -> p.getOrder() < session.getPresetInfo().getSfuPublisherThreshold())
                .sorted(Comparator.comparing(Participant::getOrder))
                .collect(Collectors.toList());

        int order = 0;
        List<CompositeObjectWrapper> source = new ArrayList<>(parts.size());
        JsonArray passThruList = new JsonArray();

        for (Participant part : parts) {
            StreamType streamType = StreamType.MAJOR;
            if (part.getTerminalType() == TerminalTypeEnum.HDC) {
                streamType = parts.size() <= 4 ? StreamType.MAJOR : StreamType.MINOR;
            }
            passThruList.add(constructPartRecordInfo(part, source, streamType, order++));
        }
        this.sourcesPublisher = source;
        this.passThruList = passThruList;
    }

    private void setLayoutCoordinates(JsonArray layoutInfos) {
        JsonArray jsonElements = layoutInfos.deepCopy();
        for (JsonElement jsonElement : jsonElements) {
            JsonObject jo = jsonElement.getAsJsonObject();
            jo.remove("object");
        }
        this.layoutCoordinates = jsonElements;
    }

    private JsonObject constructPartRecordInfo(Participant part, List<CompositeObjectWrapper> source, StreamType streamType, int order) {
        KurentoParticipant kurentoParticipant = (KurentoParticipant) part;
        log.info("record construct participant:{}, uuid:{}, osd:{}, order:{}, role:{}, handStatus:{}, streamType:{}, record info.",
                part.getParticipantPublicId(), part.getUuid(), part.getUsername(), order, part.getRole().name(), part.getHandStatus().name(),streamType);

        PublisherEndpoint publisherEndpoint = kurentoParticipant.getPublisher(streamType);
        // 如果子流是空的，则转主流在尝试一次
        if (publisherEndpoint == null && streamType == StreamType.MINOR) {
            streamType = StreamType.MAJOR;
            publisherEndpoint = kurentoParticipant.getPublisher(streamType);
        }
        if (Objects.isNull(publisherEndpoint) || Objects.isNull(publisherEndpoint.getPassThru())) {
            publisherEndpoint = kurentoParticipant.createPublisher(streamType);
            publisherEndpoint.setPassThru(new PassThrough.Builder(this.session.getPipeline()).build());
            kurentoParticipant.setPublisher(streamType, publisherEndpoint);
        }
        JsonObject jsonObject = new JsonObject();
        if (Objects.nonNull(publisherEndpoint.getPassThru())) {
            jsonObject.addProperty("passThruId", publisherEndpoint.getPassThru().getId());
            jsonObject.addProperty("order", order);
            jsonObject.addProperty("uuid", part.getUuid());
            jsonObject.addProperty("streamType", streamType.name());
            jsonObject.addProperty("osd", part.getUsername());
        }
        source.add(new CompositeObjectWrapper(part, streamType, publisherEndpoint));
        return jsonObject;
    }

    private static class CompositeObjectWrapper {
        String uuid;
        String username;
        int order;
        StreamType streamType;
        String streamId;
        PublisherEndpoint endpoint;

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
        public String toString() {
            return "CompositeObjectWrapper{" +
                    "uuid='" + uuid + '\'' +
                    ", username='" + username + '\'' +
                    ", order=" + order +
                    ", streamType=" + streamType +
                    ", streamId='" + streamId + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CompositeObjectWrapper that = (CompositeObjectWrapper) o;
            return order == that.order &&
                    Objects.equals(uuid, that.uuid) &&
                    Objects.equals(username, that.username) &&
                    streamType == that.streamType &&
                    Objects.equals(streamId, that.streamId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid, username, order, streamType, streamId);
        }
    }
}
