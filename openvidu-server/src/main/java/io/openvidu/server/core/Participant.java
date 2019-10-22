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
import io.openvidu.server.common.enums.*;
import io.openvidu.server.utils.GeoLocation;

public class Participant {

	protected String finalUserId; // ID to match this connection with a final user (HttpSession id)
	protected String participantPrivatetId; // ID to identify the user on server (org.kurento.jsonrpc.Session.id)
	protected String participantPublicId; // ID to identify the user on clients
	private String sessionId; // ID of the session to which the participant belongs
	protected Long createdAt; // Timestamp when this connection was established
	protected String clientMetadata = ""; // Metadata provided on client side
	protected String serverMetadata = ""; // Metadata provided on server side
//	protected Token token; // Token associated to this participant
	private OpenViduRole role;
	private StreamType streamType;
	protected GeoLocation location; // Location of the participant
	protected String platform; // Platform used by the participant to connect to the session

	protected boolean streaming = false;
	protected volatile boolean closed;

	protected ParticipantHandStatus handStatus;
	protected ParticipantMicStatus micStatus;
	protected ParticipantSharePowerStatus sharePowerStatus;
	protected ParticipantVideoStatus videoStatus;
	protected String roomSubject;

	protected String appShowName;
	protected String appShowDesc;
	protected ParticipantSpeakerStatus speakerStatus;
	protected ParticipantShareStatus shareStatus;
	protected SessionPreset preset;

	private final String METADATA_SEPARATOR = "%/%";
    protected static final Gson gson = new GsonBuilder().create();

	public Participant(String finalUserId, String participantPrivatetId, String participantPublicId, String sessionId, OpenViduRole role,
					   StreamType streamType, String clientMetadata, GeoLocation location, String platform, Long createdAt) {
		this.finalUserId = finalUserId;
		this.participantPrivatetId = participantPrivatetId;
		this.participantPublicId = participantPublicId;
		this.sessionId = sessionId;
		if (createdAt != null) {
			this.createdAt = createdAt;
		} else {
			this.createdAt = System.currentTimeMillis();
		}
//		this.token = token;
		this.clientMetadata = clientMetadata;
		/*if (!token.getServerMetadata().isEmpty())
			this.serverMetadata = token.getServerMetadata();*/
		this.role = role;
		this.streamType = streamType;
		this.location = location;
		this.platform = platform;
		this.handStatus = ParticipantHandStatus.down;
		this.micStatus = ParticipantMicStatus.off;
		this.sharePowerStatus = ParticipantSharePowerStatus.off;
		this.videoStatus = ParticipantVideoStatus.on;
		this.speakerStatus = ParticipantSpeakerStatus.on;
		this.shareStatus = ParticipantShareStatus.off;
	}

	public String getFinalUserId() {
		return finalUserId;
	}

	public String getParticipantPrivateId() {
		return participantPrivatetId;
	}

	public void setParticipantPrivateId(String participantPrivateId) {
		this.participantPrivatetId = participantPrivateId;
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

//	public Token getToken() {
//		return this.token;
//	}
//
//	public void setToken(Token token) {
//		this.token = token;
//	}

	public OpenViduRole getRole() {
		return role;
	}

	// 仅仅在 主持人和发布者之间角色切换
	public void setRole(OpenViduRole role) {
		this.role = role;
	}

	public StreamType getStreamType() {
		return streamType;
	}

	public ParticipantHandStatus getHandStatus() {
		return handStatus;
	}

	public void setHandStatus(ParticipantHandStatus handStatus) {
		this.handStatus = handStatus;
	}

	public ParticipantMicStatus getMicStatus() { return micStatus; }

	public void setMicStatus(ParticipantMicStatus micStatus) { this.micStatus = micStatus; }

	public ParticipantVideoStatus getVideoStatus() { return videoStatus; }

	public void setVideoStatus(ParticipantVideoStatus status) { this.videoStatus = status; }

	public ParticipantSharePowerStatus getSharePowerStatus() { return sharePowerStatus; }

	public void setSharePowerStatus(ParticipantSharePowerStatus status) { this.sharePowerStatus = status; }

	public String getRoomSubject() { return this.roomSubject; }

	public void setRoomSubject(String subject) { this.roomSubject = subject; }

	public String getAppShowName() { return this.appShowName; }

	public void setAppShowName(String appShowName) { this.appShowName = appShowName; }

	public String getAppShowDesc() { return this.appShowDesc; }

	public void setAppShowDesc(String appShowDesc) { this.appShowDesc = appShowDesc; }

	public void setAppShowInfo(String appShowName, String appShowDesc) { setAppShowName(appShowName); setAppShowDesc(appShowDesc);}

	public void setSpeakerStatus(ParticipantSpeakerStatus status) { this.speakerStatus = status; }

	public ParticipantSpeakerStatus getSpeakerStatus() { return this.speakerStatus; }

	public void setShareStatus(ParticipantShareStatus status) { this.shareStatus = status; }

	public ParticipantShareStatus getShareStatus() { return this.shareStatus; }

	public void setPreset(SessionPreset preset) { this.preset = preset; }

	public SessionPreset getPreset() { return this.preset; }

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

	public boolean isStreaming() {
		return streaming;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setStreaming(boolean streaming) {
		this.streaming = streaming;
	}

	public String getPublisherStreamId() {
		return null;
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
		result = prime * result + (participantPrivatetId == null ? 0 : participantPrivatetId.hashCode());
		result = prime * result + (streaming ? 1231 : 1237);
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
		if (participantPrivatetId == null) {
			if (other.participantPrivatetId != null) {
				return false;
			}
		} else if (!participantPrivatetId.equals(other.participantPrivatetId)) {
			return false;
		}
		if (streaming != other.streaming) {
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
		if (participantPrivatetId != null) {
			builder.append("participantPrivateId=").append(participantPrivatetId).append(", ");
		}
		if (participantPublicId != null) {
			builder.append("participantPublicId=").append(participantPublicId).append(", ");
		}
		builder.append("streaming=").append(streaming).append("]");
		return builder.toString();
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.addProperty("connectionId", this.participantPublicId);
		json.addProperty("createdAt", this.createdAt);
		json.addProperty("location", this.location != null ? this.location.toString() : "unknown");
		json.addProperty("platform", this.platform);
//		json.addProperty("token", this.token.getToken());
		json.addProperty("role", this.getRole().name());
		json.addProperty("serverData", this.serverMetadata);
		json.addProperty("clientData", this.clientMetadata);
		return json;
	}

}
