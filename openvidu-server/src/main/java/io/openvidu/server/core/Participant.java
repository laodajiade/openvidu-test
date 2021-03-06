/*
 * (C) Copyright 2017-2019 OpenVidu (https://openvidu.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.openvidu.server.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.events.ParticipantStatusChangeEvent;
import io.openvidu.server.common.events.StatusEvent;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.kurento.endpoint.SubscriberEndpoint;
import io.openvidu.server.utils.GeoLocation;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class Participant {

    protected String participantPrivateId; // ID to identify the user on server (org.kurento.jsonrpc.Session.id)
    protected String participantPublicId; // ID to identify the user on clients
    final private String sessionId; // ID of the session to which the participant belongs
    protected Long createdAt; // Timestamp when this connection was established
    protected String clientMetadata = ""; // Metadata provided on client side
    protected String serverMetadata = ""; // Metadata provided on server side
    private OpenViduRole role;
    // delete by 2.0, streamType 流类型已经转移到publishers中。part不在拥有流状态，现在每个session每个人只有一个part
    //private StreamType streamType;
    protected GeoLocation location; // Location of the participant
    protected String platform; // Platform used by the participant to connect to the session

    @Getter
    protected String deviceModel;
    //delete 2.0 与会者的流状态需要修改
    //protected boolean streaming = false;
    protected volatile boolean closed;

    private Long userId;

    @Getter
    @Setter
    private String uuid;
    @Getter
    protected int order;

    @Getter
    @Setter
    private String username;
    private String participantName;
    protected ParticipantHandStatus handStatus;
    protected ParticipantMicStatus micStatus;
    protected ParticipantVideoStatus videoStatus;
    protected String roomSubject;

    protected String appShowName;
    protected String appShowDesc;
    protected ParticipantSpeakerStatus speakerStatus;
    protected ParticipantShareStatus shareStatus;
    protected SessionPreset preset;
    protected ParticipantJoinType joinType;
    private String ability;
    @Getter
    @Setter
    private String functionality;

    @Getter
    @Setter
    private String project;

    @Getter
    @Setter
    private UserType userType = UserType.register;

    @Getter
    @Setter
    private TerminalTypeEnum terminalType;

    private VoiceMode voiceMode = VoiceMode.off;

    private SubtitleConfigEnum subtitleConfig = SubtitleConfigEnum.Off;
    private SubtitleLanguageEnum subtitleLanguage = SubtitleLanguageEnum.cn;

    @Getter
    @Setter
    private ApplicationContext applicationContext;

    @Getter
    @Setter
    private String usedRTCMode;

    private final String METADATA_SEPARATOR = "%/%";
    protected static final Gson gson = new GsonBuilder().create();

    public Participant(Long userId, String participantPrivatetId, String participantPublicId, String sessionId, OpenViduRole role,
                       String clientMetadata, GeoLocation location, String platform, String deviceModel, Long createdAt, String ability, String functionality) {
        this.participantPrivateId = participantPrivatetId;
        this.participantPublicId = participantPublicId;
        this.sessionId = sessionId;
        if (createdAt != null) {
            this.createdAt = createdAt;
        } else {
            this.createdAt = System.currentTimeMillis();
        }
        this.clientMetadata = clientMetadata;
        this.userId = userId;
        this.role = role;
        //this.streamType = streamType;
        this.location = location;
        this.platform = platform;
        this.deviceModel = deviceModel;
        this.handStatus = ParticipantHandStatus.down;
        this.videoStatus = ParticipantVideoStatus.on;
        this.micStatus = ParticipantMicStatus.on;
        this.speakerStatus = ParticipantSpeakerStatus.on;
        this.shareStatus = ParticipantShareStatus.off;
        this.joinType = ParticipantJoinType.active;
        this.ability = ability;
    }

    public void changePartRole(OpenViduRole role) {
        setRole(role);
        JsonObject clientMetadataObj = new Gson().fromJson(clientMetadata, JsonObject.class);
        clientMetadataObj.addProperty("role", role.name());
        setClientMetadata(clientMetadataObj.toString());
    }

    public String getParticipantPrivateId() {
        return participantPrivateId;
    }

    public void setParticipantPrivateId(String participantPrivateId) {
        this.participantPrivateId = participantPrivateId;
    }

    public String getParticipantPublicId() {
        return participantPublicId;
    }

    public void setParticipantPublicId(String participantPublicId) {
        this.participantPublicId = participantPublicId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantName() {
        return this.participantName;
    }

    public Long getCreatedAt() {
        return this.createdAt;
    }

    public String getClientMetadata() {
        return clientMetadata;
    }

    public void setClientMetadata(String clientMetadata) {
        this.clientMetadata = clientMetadata;
    }

    public String getServerMetadata() {
        return serverMetadata;
    }

    public void setServerMetadata(String serverMetadata) {
        this.serverMetadata = serverMetadata;
    }

    public OpenViduRole getRole() {
        return role;
    }

    // 仅仅在 主持人和发布者之间角色切换
    public void setRole(OpenViduRole role) {
        this.role = role;
    }

//	public StreamType getStreamType() {
//		return streamType;
//	}

    public ParticipantHandStatus getHandStatus() {
        return handStatus;
    }

    public void changeHandStatus(ParticipantHandStatus handStatus) {
        setHandStatus(handStatus);
        applicationContext.publishEvent(new ParticipantStatusChangeEvent(StatusEvent.builder()
                .sessionId(sessionId).uuid(uuid).field("handStatus").updateStatus(handStatus.name()).build()));
    }

    public void setHandStatus(ParticipantHandStatus handStatus) {
        this.handStatus = handStatus;
    }

    public ParticipantMicStatus getMicStatus() {
        return micStatus;
    }

    public void changeMicStatus(ParticipantMicStatus micStatus) {
        setMicStatus(micStatus);
        applicationContext.publishEvent(new ParticipantStatusChangeEvent(StatusEvent.builder()
                .sessionId(sessionId).uuid(uuid).field("micStatus").updateStatus(micStatus.name()).build()));
    }

    public void setMicStatus(ParticipantMicStatus micStatus) {
        this.micStatus = micStatus;
    }

    public ParticipantVideoStatus getVideoStatus() {
        return videoStatus;
    }

    public void changeVideoStatus(ParticipantVideoStatus status) {
        setVideoStatus(status);
        applicationContext.publishEvent(new ParticipantStatusChangeEvent(StatusEvent.builder()
                .sessionId(sessionId).uuid(uuid).field("videoStatus").updateStatus(status.name()).build()));
    }

    public void setVideoStatus(ParticipantVideoStatus status) {
        this.videoStatus = status;
    }

    public String getRoomSubject() {
        return this.roomSubject;
    }

    public void setRoomSubject(String subject) {
        this.roomSubject = subject;
    }

    public String getAppShowName() {
        return this.appShowName;
    }

    public void setAppShowName(String appShowName) {
        this.appShowName = appShowName;
    }

    public String getAppShowDesc() {
        return this.appShowDesc;
    }

    public void setAppShowDesc(String appShowDesc) {
        this.appShowDesc = appShowDesc;
    }

    public void setAppShowInfo(String appShowName, String appShowDesc) {
        setAppShowName(appShowName);
        setAppShowDesc(appShowDesc);
    }

    public void changeSpeakerStatus(ParticipantSpeakerStatus status) {
        setSpeakerStatus(status);
        applicationContext.publishEvent(new ParticipantStatusChangeEvent(StatusEvent.builder()
                .sessionId(sessionId).uuid(uuid).field("speakerStatus").updateStatus(status.name()).build()));
    }

    public void setSpeakerStatus(ParticipantSpeakerStatus status) {
        this.speakerStatus = status;
    }

    public ParticipantSpeakerStatus getSpeakerStatus() {
        return this.speakerStatus;
    }

    public void changeShareStatus(ParticipantShareStatus status) {
        setShareStatus(status);
        applicationContext.publishEvent(new ParticipantStatusChangeEvent(StatusEvent.builder()
                .sessionId(sessionId).uuid(uuid).field("shareStatus").updateStatus(status.name()).build()));
    }

    public void setShareStatus(ParticipantShareStatus status) {
        this.shareStatus = status;
    }

    public ParticipantShareStatus getShareStatus() {
        return this.shareStatus;
    }

    public void setJoinType(ParticipantJoinType joinType) {
        this.joinType = joinType;
    }

    public ParticipantJoinType getJoinType() {
        return this.joinType;
    }

    public String getAbility() {
        return ability;
    }

    public void setAbility(String ability) {
        this.ability = ability;
    }

    public void setPreset(SessionPreset preset) {
        this.preset = preset;
    }

    public SessionPreset getPreset() {
        return this.preset;
    }

    public GeoLocation getLocation() {
        return this.location;
    }

    public void setLocation(GeoLocation location) {
        this.location = location;
    }

    public String getPlatform() {
        return this.platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    //delete 2.0 Deprecated
//    @Deprecated
//    public boolean isStreaming() {
//        return streaming;
//    }

    public boolean isStreaming(StreamType streamType) {
        PublisherEndpoint publisher = this.getPublisher(streamType);
        if (publisher == null) {
            return false;
        }
        return publisher.isStreaming();
    }

    public boolean isClosed() {
        return closed;
    }

    //delete 2.0 Deprecated
//    public void setStreaming(boolean streaming) {
//        this.streaming = streaming;
//    }

    public SubtitleConfigEnum getSubtitleConfig() {
        return subtitleConfig;
    }

    public Participant setSubtitleConfig(SubtitleConfigEnum subtitleConfig) {
        this.subtitleConfig = subtitleConfig;
        return this;
    }

    public SubtitleLanguageEnum getSubtitleLanguage() {
        return subtitleLanguage;
    }

    public Participant setSubtitleLanguage(SubtitleLanguageEnum subtitleLanguage) {
        this.subtitleLanguage = subtitleLanguage;
        return this;
    }

    public VoiceMode getVoiceMode() {
        return voiceMode;
    }

    //    public void setVoiceMode(VoiceMode voiceMode) {
//        this.voiceMode = voiceMode;
//    }
    public void changeVoiceMode(VoiceMode voiceMode) {
        this.voiceMode = voiceMode;
        applicationContext.publishEvent(new ParticipantStatusChangeEvent(StatusEvent.builder()
                .sessionId(sessionId).uuid(uuid).field("voiceMode").updateStatus(voiceMode.name()).build()));
    }

    //2.0 Deprecated
    @Deprecated
    public String getPublisherStreamId() {
        return null;
    }

    public PublisherEndpoint getPublisher(StreamType streamType) {
        return null;
    }

    public ConcurrentMap<String, SubscriberEndpoint> getSubscribers() {
        return null;
    }

    /**
     * 获取混流的订阅
     */
    public SubscriberEndpoint getMixSubscriber() {
        for (Map.Entry<String, SubscriberEndpoint> entry : getSubscribers().entrySet()) {
            if (entry.getKey().contains(CommonConstants.MIX_STREAM_ID_TRAIT)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 查询订阅
     */
    public SubscriberEndpoint getSubscriber(String subscribeId) {
        return getSubscribers().get(subscribeId);
    }

    public String getFullMetadata() {
        String fullMetadata;
        JsonObject clientMetaJson = gson.fromJson(clientMetadata, JsonObject.class);
        clientMetaJson.addProperty("role", this.role.name());
        if ((!this.clientMetadata.isEmpty()) && (!this.serverMetadata.isEmpty())) {
            fullMetadata = clientMetaJson.toString() + METADATA_SEPARATOR + this.serverMetadata;
        } else {
            fullMetadata = clientMetaJson.toString() + this.serverMetadata;
        }
        return fullMetadata;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (participantPrivateId == null ? 0 : participantPrivateId.hashCode());
        result = prime * result + (participantPublicId == null ? 0 : participantPublicId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Participant)) {
            return false;
        }
        Participant other = (Participant) obj;
        if (participantPrivateId == null) {
            if (other.participantPrivateId != null) {
                return false;
            }
        } else if (!participantPrivateId.equals(other.participantPrivateId)) {
            return false;
        }
        if (participantPublicId == null) {
            if (other.participantPublicId != null) {
                return false;
            }
        } else if (!participantPublicId.equals(other.participantPublicId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if (participantPrivateId != null) {
            builder.append("participantPrivateId=").append(participantPrivateId).append(", ");
        }
        if (participantPublicId != null) {
            builder.append("participantPublicId=").append(participantPublicId).append(", ");
        }
        return builder.toString();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", this.uuid);
        json.addProperty("connectionId", this.participantPublicId);
        json.addProperty("createdAt", this.createdAt);
        json.addProperty("location", this.location != null ? this.location.toString() : "unknown");
        json.addProperty("platform", this.platform);
        json.addProperty("role", this.getRole().name());
        json.addProperty("serverData", this.serverMetadata);
        json.addProperty("clientData", this.clientMetadata);
        return json;
    }

    public boolean ableToUpdateRecord() {
        return getRole().needToPublish();
    }

    public void setOrder(int order) {
        this.order = order;
        applicationContext.publishEvent(new ParticipantStatusChangeEvent(StatusEvent.builder()
                .sessionId(sessionId).uuid(uuid).field("order").updateStatus(order).build()));
    }
}
