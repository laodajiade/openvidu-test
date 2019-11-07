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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.Recording;
import io.openvidu.java.client.RecordingLayout;
import io.openvidu.java.client.SessionProperties;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.recording.service.RecordingManager;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Session implements SessionInterface {

	protected OpenviduConfig openviduConfig;
	protected RecordingManager recordingManager;

//	protected final ConcurrentMap<String, Participant> participants = new ConcurrentHashMap<>();
	protected final ConcurrentMap<String, ConcurrentMap<String, Participant>> participants = new ConcurrentHashMap<>();
	protected String sessionId;
	protected SessionProperties sessionProperties;
	protected Long startTime;
	// TODO. Maybe we should relate conference in here.
	protected Conference conference;
	protected SessionPreset preset;
	protected LayoutModeEnum layoutMode;
	protected JsonArray layoutInfo;
	protected int delayConfCnt;
	protected int delayTimeUnit = 20 * 60;	// default 20min
	protected boolean notifyCountdown10Min = false;
	protected boolean notifyCountdown1Min = false;

	protected volatile boolean closed = false;
	private volatile boolean locking = false;
	protected AtomicInteger activePublishers = new AtomicInteger(0);

	public final AtomicBoolean recordingManuallyStopped = new AtomicBoolean(false);

	public Session(Session previousSession) {
		this.sessionId = previousSession.getSessionId();
		this.startTime = previousSession.getStartTime();
		this.sessionProperties = previousSession.getSessionProperties();
		this.openviduConfig = previousSession.openviduConfig;
		this.recordingManager = previousSession.recordingManager;

		this.conference = previousSession.conference;
		this.preset = previousSession.preset;
		this.layoutMode = previousSession.getLayoutMode();
		this.layoutInfo = previousSession.getLayoutInfo();
		this.delayConfCnt = previousSession.delayConfCnt;
	}

	public Session(String sessionId, SessionProperties sessionProperties, OpenviduConfig openviduConfig,
			RecordingManager recordingManager) {
		this.sessionId = sessionId;
		this.startTime = System.currentTimeMillis();
		this.sessionProperties = sessionProperties;
		this.openviduConfig = openviduConfig;
		this.recordingManager = recordingManager;

		this.layoutMode = LayoutModeEnum.DEFAULT;
		this.delayConfCnt = 0;
		this.delayTimeUnit = openviduConfig.getVoipDelayUnit() * 60;	// default 20min
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public SessionProperties getSessionProperties() {
		return this.sessionProperties;
	}

	public Long getStartTime() {
		return this.startTime;
	}

	public void setConference(Conference conference){ this.conference = conference; }

	public Conference getConference() { return this.conference; }

	public void setPresetInfo(SessionPreset preset) { this.preset = preset; }

	public SessionPreset getPresetInfo() { return this.preset; }

	public LayoutModeEnum getLayoutMode() {
		return layoutMode;
	}

	public void setLayoutMode(LayoutModeEnum layoutMode) {
		this.layoutMode = layoutMode;
	}

	public JsonArray getLayoutInfo() {
		return layoutInfo;
	}

	public void setLayoutInfo(JsonArray layoutInfo) {
		this.layoutInfo = layoutInfo;
	}

	public void setDelayConfCnt(int delayConfCnt) { this.delayConfCnt = delayConfCnt; }

	public int incDelayConfCnt() { return this.delayConfCnt++; }

	public int getDelayConfCnt() { return this.delayConfCnt; }

	public void setNotifyCountdown10Min(boolean notifyCountdown10Min) { this.notifyCountdown10Min = notifyCountdown10Min; }

	public boolean getNotifyCountdown10Min() { return this.notifyCountdown10Min; }

	public void setNotifyCountdown1Min(boolean notifyCountdown1Min) { this.notifyCountdown1Min = notifyCountdown1Min; }

	public boolean getNotifyCountdown1Min() { return this.notifyCountdown1Min; }

	public int getConfDelayTime() { return this.delayConfCnt * this.delayTimeUnit; }

	public long getConfStartTime() {						// unit is ms
		return getConference().getStartTime().getTime();
	}

	public long getConfEndTime() {							// unit is ms
		int confDuration = Float.valueOf(getPresetInfo().getRoomDuration() * 60 * 60 * 1000).intValue() + getConfDelayTime() * 1000;
		return getConfStartTime() + confDuration;
	}

	public long getConfRemainTime() {						// unit is second
		return (getConfEndTime() - new Date().getTime()) / 1000;
	}

	/*public Set<Participant> getParticipants() {
		checkClosed();
		return new HashSet<Participant>(this.participants.values());
	}*/

	public Set<Participant> getParticipants() {
		checkClosed();
		return this.participants.values().stream().flatMap(v ->
				v.values().stream()).collect(Collectors.toSet());
	}

	public Set<Participant> getMajorPartEachConnect() {
		checkClosed();
		return this.participants.values().stream().map(v ->
				v.get(StreamType.MAJOR.name())).collect(Collectors.toSet());
	}

    /*public Participant getParticipantByPrivateId(String participantPrivateId) {
        checkClosed();
        return participants.get(participantPrivateId);
    }*/

    public Participant getParticipantByPrivateId(String participantPrivateId) {
        checkClosed();

		if (Objects.isNull(participants.get(participantPrivateId))) {
			return null;
		}

        return participants.get(participantPrivateId).get(StreamType.MAJOR.name());
    }

    public Participant getPartByPrivateIdAndStreamType(String participantPrivateId, StreamType streamType) {
        checkClosed();

        if (Objects.isNull(participants.get(participantPrivateId))) {
        	return null;
		}

        return Objects.isNull(streamType) ? participants.get(participantPrivateId).get(StreamType.MAJOR.name()) :
				participants.get(participantPrivateId).get(streamType.name());
    }

    public Participant getPartByPrivateIdAndPublicId(String participantPrivateId, String participantPublicId) {
        checkClosed();

        if (Objects.isNull(participants.get(participantPrivateId))) {
            return null;
        }

        if (participants.get(participantPrivateId).values().size() <= 1) {
            return getParticipantByPrivateId(participantPrivateId);
        }

        for (Participant p: participants.get(participantPrivateId).values()) {
            if (p.getParticipantPublicId().equals(participantPublicId)) {
                return p;
            }
        }

		return participants.get(participantPrivateId).get(StreamType.MAJOR.name());
    }

	public Participant getParticipantByPublicId(String participantPublicId) {
		checkClosed();
		for (Participant p : getParticipants()) {
			if (p.getParticipantPublicId().equals(participantPublicId)) {
				return p;
			}
		}
		return null;
	}

	public int getActivePublishers() {
		return activePublishers.get();
	}

	public void registerPublisher() {
		this.activePublishers.incrementAndGet();
	}

	public void deregisterPublisher() {
		this.activePublishers.decrementAndGet();
	}

	public boolean isClosed() {
		return closed;
	}

	protected void checkClosed() {
		if (isClosed()) {
			throw new OpenViduException(Code.ROOM_CLOSED_ERROR_CODE, "The session '" + sessionId + "' is closed");
		}
	}

	public boolean isLocking() {
		return locking;
	}

	public boolean setLocking(boolean locking) {
		this.locking = locking;
		return this.locking;
	}

	public JsonObject toJson() {
		return this.sharedJson(KurentoParticipant::toJson);
	}

	public JsonObject withStatsToJson() {
		return this.sharedJson(KurentoParticipant::withStatsToJson);
	}

	private JsonObject sharedJson(Function<KurentoParticipant, JsonObject> toJsonFunction) {
		JsonObject json = new JsonObject();
		json.addProperty("sessionId", this.sessionId);
		json.addProperty("createdAt", this.startTime);
		json.addProperty("mediaMode", this.sessionProperties.mediaMode().name());
		json.addProperty("recordingMode", this.sessionProperties.recordingMode().name());
		json.addProperty("defaultOutputMode", this.sessionProperties.defaultOutputMode().name());
		if (Recording.OutputMode.COMPOSED.equals(this.sessionProperties.defaultOutputMode())) {
			json.addProperty("defaultRecordingLayout", this.sessionProperties.defaultRecordingLayout().name());
			if (RecordingLayout.CUSTOM.equals(this.sessionProperties.defaultRecordingLayout())) {
				json.addProperty("defaultCustomLayout", this.sessionProperties.defaultCustomLayout());
			}
		}
		if (this.sessionProperties.customSessionId() != null) {
			json.addProperty("customSessionId", this.sessionProperties.customSessionId());
		}
		JsonObject connections = new JsonObject();
		JsonArray participants = new JsonArray();
		/*this.participants.values().forEach(p -> {
			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(p.getParticipantPublicId())) {
				participants.add(toJsonFunction.apply((KurentoParticipant) p));
			}
		});*/
		this.participants.values().stream().flatMap(m -> m.values().stream()).forEach(p -> {
            if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(p.getParticipantPublicId())) {
                participants.add(toJsonFunction.apply((KurentoParticipant) p));
            }
        });
		connections.addProperty("numberOfElements", participants.size());
		connections.add("content", participants);
		json.add("connections", connections);
		json.addProperty("recording", this.recordingManager.sessionIsBeingRecorded(this.sessionId));
		return json;
	}

	@Override
	public void join(Participant participant) {
	}

	@Override
	public void leave(String participantPrivateId, EndReason reason) {
	}

    @Override
    public void leaveRoom(Participant p, EndReason reason) {
    }

	@Override
	public boolean close(EndReason reason) {
		return false;
	}

}
