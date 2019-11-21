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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.Recording;
import io.openvidu.java.client.RecordingLayout;
import io.openvidu.java.client.SessionProperties;
import io.openvidu.server.common.enums.LayoutChangeTypeEnum;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.layout.LayoutInitHandler;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.recording.service.RecordingManager;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.KurentoClient;
import org.kurento.jsonrpc.message.Request;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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
	protected JsonArray layoutCoordinates = LayoutInitHandler.getLayoutByMode(LayoutModeEnum.FOUR);
	protected LayoutChangeTypeEnum layoutChangeTypeEnum;
	protected JsonArray layoutInfo = new JsonArray(1);
	protected int delayConfCnt;
	protected int delayTimeUnit = 20 * 60;	// default 20min
	protected boolean notifyCountdown10Min = false;
	protected boolean notifyCountdown1Min = false;

	protected volatile boolean closed = false;
	private volatile boolean locking = false;
	protected AtomicInteger activePublishers = new AtomicInteger(0);

	public final AtomicBoolean recordingManuallyStopped = new AtomicBoolean(false);

	protected JsonArray majorMixLinkedArr = new JsonArray(50);
	protected JsonArray majorShareMixLinkedArr = new JsonArray(50);

	public Session(Session previousSession) {
		this.sessionId = previousSession.getSessionId();
		this.startTime = previousSession.getStartTime();
		this.sessionProperties = previousSession.getSessionProperties();
		this.openviduConfig = previousSession.openviduConfig;
		this.recordingManager = previousSession.recordingManager;

		this.conference = previousSession.conference;
		this.preset = previousSession.preset;
		this.layoutMode = previousSession.getLayoutMode();
		this.layoutChangeTypeEnum = previousSession.getLayoutChangeTypeEnum();
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

		this.layoutMode = LayoutModeEnum.FOUR;
		this.layoutChangeTypeEnum = LayoutChangeTypeEnum.change;
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

	public JsonArray getLayoutCoordinates() {
		return layoutCoordinates;
	}

	public void setLayoutCoordinates(JsonArray layoutCoordinates) {
		this.layoutCoordinates = layoutCoordinates;
	}

	public LayoutChangeTypeEnum getLayoutChangeTypeEnum() {
		return layoutChangeTypeEnum;
	}

	public void setLayoutChangeTypeEnum(LayoutChangeTypeEnum layoutChangeTypeEnum) {
		this.layoutChangeTypeEnum = layoutChangeTypeEnum;
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

	public JsonArray getMajorMixLinkedArr() {
		return majorMixLinkedArr;
	}

	public JsonArray getMajorShareMixLinkedArr() {
		return majorShareMixLinkedArr;
	}

	protected void dealParticipantDefaultOrder(KurentoParticipant kurentoParticipant) {
		if (Objects.equals(StreamType.SHARING, kurentoParticipant.getStreamType())) {
			JsonArray newMajorMixLinkedArr = new JsonArray(50);
			int size = majorShareMixLinkedArr.size();
			if (size == 1) {
				majorShareMixLinkedArr.add(getPartLayoutInfo(1, StreamType.SHARING.name(),
						kurentoParticipant.getParticipantPublicId()));
			} else {
				for (int i = 0; i < size; i++) {
					if (i == 1) {
						newMajorMixLinkedArr.add(getPartLayoutInfo(1, StreamType.SHARING.name(),
								kurentoParticipant.getParticipantPublicId()));

						JsonObject originObj = majorShareMixLinkedArr.get(i).getAsJsonObject();
						newMajorMixLinkedArr.add(getPartLayoutInfo(2, originObj.get("streamType").getAsString(),
								originObj.get("connectionId").getAsString()));
					} else {
						JsonObject originObj = majorShareMixLinkedArr.get(i).getAsJsonObject();
						int k = (i == 0) ? i : (i + 1);
						newMajorMixLinkedArr.add(getPartLayoutInfo(k, originObj.get("streamType").getAsString(),
								originObj.get("connectionId").getAsString()));
					}
				}
				majorShareMixLinkedArr = newMajorMixLinkedArr;
			}
		} else {
			majorMixLinkedArr.add(getPartLayoutInfo(majorMixLinkedArr.size(), StreamType.MAJOR.name(),
					kurentoParticipant.getParticipantPublicId()));

			majorShareMixLinkedArr.add(getPartLayoutInfo(majorShareMixLinkedArr.size(), StreamType.MAJOR.name(),
					kurentoParticipant.getParticipantPublicId()));
		}

		log.info("dealParticipantDefaultOrder majorMixLinkedArr:{}", majorMixLinkedArr.toString());
        log.info("dealParticipantDefaultOrder majorShareMixLinkedArr:{}", majorShareMixLinkedArr.toString());
	}

	public void  leaveRoomSetLayout(StreamType streamType, Participant participant){
		int countShareSeat = 0; //The position of the person leaving the meeting in the split screen
		int countSeat = 0;
		for (JsonElement element : majorShareMixLinkedArr) {
			JsonObject jsonObject = element.getAsJsonObject();
			countShareSeat++;
			if (Objects.equals(jsonObject.get("connectionId").getAsString(), participant.getParticipantPublicId())) {
				break;
			}
		}
		for (JsonElement e : majorMixLinkedArr) {
			JsonObject json = e.getAsJsonObject();
			countSeat++;
			if (Objects.equals(json.get("connectionId").getAsString(), participant.getParticipantPublicId())) {
				break;
			}
		}
		JsonArray newMajorMixLinkedArr = new JsonArray(50);
		JsonArray newMajorShareMixLinkedArr = new JsonArray(50);
		int size = majorMixLinkedArr.size();
		int Sharesize = majorShareMixLinkedArr.size();
		for (int j = 0 ; j < Sharesize; j++) {
			int k = j+1;
			if (j < countShareSeat){
				JsonObject jsonObject = majorShareMixLinkedArr.get(j).getAsJsonObject();
				newMajorShareMixLinkedArr.add(getPartLayoutInfo(j, jsonObject.get("streamType").getAsString(),
						jsonObject.get("connectionId").getAsString()));
			}else {
				JsonObject originObj = majorShareMixLinkedArr.get(k).getAsJsonObject();
				newMajorShareMixLinkedArr.add(getPartLayoutInfo(j, originObj.get("streamType").getAsString(),
						originObj.get("connectionId").getAsString()));
			}
		}
		if (Objects.equals(streamType, StreamType.MAJOR)) {
			for (int a = 0; a < size; a++) {
				int c = a+1;
				if (a < countSeat) {
					JsonObject jsonObject = majorMixLinkedArr.get(a).getAsJsonObject();
					newMajorMixLinkedArr.add(getPartLayoutInfo(a,jsonObject.get("streamType").getAsString(),
							jsonObject.get("connectionId").getAsString()));
				} else {
					JsonObject originObj = majorMixLinkedArr.get(c).getAsJsonObject();
					newMajorMixLinkedArr.add(getPartLayoutInfo(a, originObj.get("streamType").getAsString(), originObj.get("connectionId").getAsString()));
				}
			}
		}

		majorMixLinkedArr = newMajorMixLinkedArr;
		majorShareMixLinkedArr = newMajorShareMixLinkedArr;

		log.info("leaveRoomSetLayout majorMixLinkedArr:{}", majorMixLinkedArr.toString());
		log.info("leaveRoomSetLayout majorShareMixLinkedArr:{}", majorShareMixLinkedArr.toString());
	}

	private JsonObject getPartLayoutInfo(int layoutIndex, String streamType, String publicId) {
    	JsonObject result = layoutCoordinates.get(layoutIndex).getAsJsonObject().deepCopy();
		log.info("layoutCoordinates.size{}", layoutCoordinates.size(), "layoutIndex{}", layoutIndex);
    	result.addProperty("streamType", streamType);
		result.addProperty("connectionId", publicId);
		return result;
	}

    public void switchLayoutMode(LayoutModeEnum layoutModeEnum) {
        setLayoutCoordinates(LayoutInitHandler.getLayoutByMode(layoutModeEnum));

        int size = layoutCoordinates.size();

        int majorSize = majorMixLinkedArr.size();
        int majorShareSize = majorShareMixLinkedArr.size();
        JsonArray newMajorMixLinkedArr = new JsonArray(50);
        JsonArray newMajorShareMixLinkedArr = new JsonArray(50);
        for (int i = 0; i < size; i++) {
            if (i >= majorSize && i >= majorShareSize) break;

            if (i < majorSize) {
                JsonObject majorJson = majorMixLinkedArr.get(i).getAsJsonObject();
                newMajorMixLinkedArr.add(getPartLayoutInfo(i, majorJson.get("streamType").getAsString(),
                        majorJson.get("connectionId").getAsString()));
            }

            if (i < majorShareSize) {
                JsonObject majorShareJson = majorShareMixLinkedArr.get(i).getAsJsonObject();
                newMajorShareMixLinkedArr.add(getPartLayoutInfo(i, majorShareJson.get("streamType").getAsString(),
                        majorShareJson.get("connectionId").getAsString()));
            }
        }

        majorMixLinkedArr = newMajorMixLinkedArr;
        majorShareMixLinkedArr = newMajorShareMixLinkedArr;

        log.info("switchLayoutMode majorMixLinkedArr:{}", majorMixLinkedArr.toString());
        log.info("switchLayoutMode majorShareMixLinkedArr:{}", majorShareMixLinkedArr.toString());
        invokeKmsConferenceLayout();
    }

	public void replacePartOrderInConference(String sourceConnectionId, String targetConnectionId) {
    	relacePartOrder(majorMixLinkedArr, sourceConnectionId, targetConnectionId);
		relacePartOrder(majorShareMixLinkedArr, sourceConnectionId, targetConnectionId);
	}

	private static void relacePartOrder(JsonArray linkedArr, String sourceConnectionId, String targetConnectionId) {
		for (JsonElement jsonElement : linkedArr) {
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			String connectionId = jsonObject.get("connectionId").getAsString();
			if (connectionId.equals(sourceConnectionId)) {
				jsonObject.addProperty("connectionId", targetConnectionId);
			} else if (connectionId.equals(targetConnectionId)) {
				jsonObject.addProperty("connectionId", sourceConnectionId);
			}
		}
	}

    public int invokeKmsConferenceLayout() {
        KurentoSession kurentoSession = (KurentoSession) this;
        KurentoClient kurentoClient = kurentoSession.getKms().getKurentoClient();
        try {
            kurentoClient.sendJsonRpcRequest(composeLayoutInvokeRequest(kurentoSession.getPipeline().getId(),
                    majorMixLinkedArr, kurentoClient.getSessionId(), true));
//            if (kurentoSession.compositeService.isExistSharing()) {
                kurentoClient.sendJsonRpcRequest(composeLayoutInvokeRequest(kurentoSession.getPipeline().getId(),
                        majorShareMixLinkedArr, kurentoClient.getSessionId(), false));
//            }
        } catch (IOException e) {
            log.error("Exception:\n", e);
            return 0;
        }

        return 1;
    }

    private Request<JsonObject> composeLayoutInvokeRequest(String pipelineId, JsonArray linkedArr, String sessionId, boolean majorComposite) {
        Request<JsonObject> kmsRequest = new Request<>();
        JsonObject params = new JsonObject();
        params.addProperty("object", pipelineId);
        params.addProperty("operation", "setLayout");
        params.addProperty("sessionId", sessionId);
        JsonArray layoutInfos = new JsonArray(50);
        for (JsonElement jsonElement : linkedArr) {
            JsonObject temp = jsonElement.getAsJsonObject();
            JsonObject resultPart = temp.deepCopy();
            KurentoParticipant kurentoParticipant = (KurentoParticipant) this.getParticipantByPublicId(temp
                    .get("connectionId").getAsString());
            if (majorComposite) {
				resultPart.addProperty("object", kurentoParticipant.getPublisher().getMajorHubPort().getId());
			} else {
				resultPart.addProperty("object", kurentoParticipant.getPublisher().getMajorShareHubPort().getId());
			}
            resultPart.addProperty("hasVideo", kurentoParticipant.getPublisherMediaOptions().hasVideo());
            resultPart.addProperty("onlineStatus",
                    kurentoParticipant.getPublisherMediaOptions().hasVideo() ? "online" : "offline");

            layoutInfos.add(resultPart);
        }
        JsonObject operationParams = new JsonObject();
        operationParams.add("layoutInfo", layoutInfos);
        params.add("operationParams", operationParams);
        kmsRequest.setMethod("invoke");
        kmsRequest.setParams(params);
        log.info("send sms setLayout params:{}", params);

        return kmsRequest;
    }

}
