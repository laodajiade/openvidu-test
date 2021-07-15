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

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.*;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.layout.LayoutInitHandler;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.CorpMcuConfig;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.kurento.core.DeliveryKmsManager;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.living.service.LivingManager;
import io.openvidu.server.recording.service.RecordingManager;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.HubPort;
import org.kurento.client.KurentoClient;
import org.kurento.jsonrpc.message.Request;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class Session implements SessionInterface {

	@Getter
	protected OpenviduConfig openviduConfig;
	protected RecordingManager recordingManager;
	protected LivingManager livingManager;

    //todo 不在使用，废弃了  participantList 代替 participants
    @Deprecated
    protected final ConcurrentMap<String, ConcurrentMap<String, Participant>> participants = new ConcurrentHashMap<>();

    /**
     * key = uuid,value = Participant
     */
    protected final ConcurrentMap<String, Participant> participantList = new ConcurrentHashMap<>();

	protected String sessionId;
	protected String traceId;
	protected String ruid;
	protected SessionProperties sessionProperties;
	protected Long startTime;
	// TODO. Maybe we should relate conference in here.
	protected Conference conference;
	protected SessionPreset preset;
	protected ConferenceModeEnum conferenceMode;
	protected boolean automatically = true;
	protected LayoutModeEnum layoutMode;
	protected JsonArray layoutCoordinates = LayoutInitHandler.getLayoutByMode(LayoutModeEnum.ONE);
	protected LayoutChangeTypeEnum layoutChangeTypeEnum;
	protected JsonArray layoutInfo = new JsonArray(1);
	protected int moderatorIndex = -1;
	protected int delayConfCnt;
	protected int delayTimeUnit = 20 * 60;	// default 20min
	protected boolean delay = false;
	protected long endTime = Long.MAX_VALUE;
	protected boolean notifyCountdown10Min = false;
	protected boolean notifyCountdown1Min = false;
	protected Long startRecordingTime;
	protected Long stopRecordingTime;
	protected Long startLivingTime;
	protected String livingUrl;
	@Getter
	@Setter
	private CorpMcuConfig corpMcuConfig;

	// 状态：会议关闭中(屏蔽布局等操作)
	protected volatile boolean closing = false;
	protected volatile boolean closed = false;
	private volatile boolean locking = false;
	protected AtomicInteger activePublishers = new AtomicInteger(0);
	public final AtomicBoolean isRecording = new AtomicBoolean(false);
	public final AtomicBoolean recordingManuallyStopped = new AtomicBoolean(false);

	public final AtomicBoolean isLiving = new AtomicBoolean(false);

	protected JsonArray majorShareMixLinkedArr = new JsonArray(50);

	// 正常与会者的排序
	protected AtomicInteger roomOrder = new AtomicInteger(-1);

	private final Object partOrderAdjustLock = new Object();

	private SubtitleConfigEnum subtitleConfig = SubtitleConfigEnum.Off;
	private Set<String> languages = new HashSet<>();
	private JsonObject subtitleExtraConfig = null;
	@Getter
	private final Lock joinOrLeaveReentrantLock = new ReentrantLock();

	@Getter
	private final Object sharingOrSpeakerLock = new Object();
	/**
	 * 分享者
	 */
	@Setter
	private Participant sharingPart = null;

	/**
	 * 发言中
	 */
	@Setter
	private Participant speakerPart = null;

	public Session(Session previousSession) {
		this.sessionId = previousSession.getSessionId();
		this.startTime = previousSession.getStartTime();
		this.sessionProperties = previousSession.getSessionProperties();
		this.openviduConfig = previousSession.openviduConfig;
		this.recordingManager = previousSession.recordingManager;
		this.livingManager = previousSession.livingManager;

		this.conference = previousSession.conference;
		this.preset = previousSession.preset;
		this.layoutMode = previousSession.getLayoutMode();
		this.layoutChangeTypeEnum = previousSession.getLayoutChangeTypeEnum();
		this.layoutInfo = previousSession.getLayoutInfo();
		this.delayConfCnt = previousSession.delayConfCnt;
		this.subtitleConfig = previousSession.getSubtitleConfig();
		this.languages = previousSession.getLanguages();
		this.corpMcuConfig = previousSession.getCorpMcuConfig();

//		this.traceId = RandomStringUtils.randomAlphabetic(6);
	}

	public Session(String sessionId, SessionProperties sessionProperties, OpenviduConfig openviduConfig,
			RecordingManager recordingManager, LivingManager livingManager) {
		this.sessionId = sessionId;
		this.startTime = System.currentTimeMillis();
		this.sessionProperties = sessionProperties;
		this.openviduConfig = openviduConfig;
		this.recordingManager = recordingManager;
		this.livingManager = livingManager;

		this.layoutMode = LayoutModeEnum.ONE;
		this.layoutChangeTypeEnum = LayoutChangeTypeEnum.change;
		this.delayConfCnt = 0;
		this.delayTimeUnit = openviduConfig.getVoipDelayUnit() * 60;	// default 20min

//		this.traceId = RandomStringUtils.randomAlphabetic(6);
	}

	public SubtitleConfigEnum getSubtitleConfig() {
		return subtitleConfig;
	}

	public void setSubtitleConfig(SubtitleConfigEnum subtitleConfig, SubtitleLanguageEnum language, JsonObject extraInfo) {
		this.subtitleConfig = subtitleConfig;
		languages.add(language.name());
		setSubtitleExtraConfig(extraInfo);
	}

	public JsonObject getSubtitleExtraConfig() {
		return subtitleExtraConfig;
	}

	public void setSubtitleExtraConfig(JsonObject subtitleExtraConfig) {
		this.subtitleExtraConfig = subtitleExtraConfig;
	}

	public Set<String> getLanguages() {
		return languages;
	}

	public String getSessionId() {
		return this.sessionId;
	}

	public String getRuid() {
		return ruid;
	}

	public void setRuid(String ruid) {
		this.ruid = ruid;
	}

	public String getTraceId() { return traceId; }

	public void setTraceId(String traceId) { this.traceId = traceId; }

	public boolean isAutomatically() {
        return automatically;
    }

    public void setAutomatically(boolean automatically) {
        this.automatically = automatically;
    }

    public SessionProperties getSessionProperties() {
		return this.sessionProperties;
	}

	public Long getStartTime() {
		return this.startTime;
	}

	private boolean isRecordingConfigured() {
		return this.openviduConfig.isRecordingModuleEnabled() && MediaMode.ROUTED.equals(sessionProperties.mediaMode());
	}

	private boolean isLivingConfigured() {
		return this.openviduConfig.isLivingModuleEnabled() && MediaMode.ROUTED.equals(sessionProperties.mediaMode());
	}

	@Override
	public boolean setIsRecording(boolean flag) {
		return isRecording.compareAndSet(!flag, flag);
	}

	@Override
	public boolean sessionAllowedStartToRecord() {
		log.info("to start recording, isRecordingConfigured:{}, session:{} is recording:{}",
				isRecordingConfigured(), sessionId, isRecording.get());
		return isRecordingConfigured() && isRecording.compareAndSet(false, true);
	}

	@Override
	public boolean sessionAllowedToStopRecording() {
		log.info("to stop recording, isRecordingConfigured:{}, session:{} is recording:{}",
				isRecordingConfigured(), sessionId, isRecording.get());
		return isRecordingConfigured() && isRecording.compareAndSet(true, false);
	}

	@Override
	public boolean setIsLiving(boolean flag) {
		log.info("session {} set isLiving:{}", this.getSessionId(), flag);
		return isLiving.compareAndSet(!flag, flag);
	}

	@Override
	public boolean sessionAllowedStartToLive() {
		log.info("to start living, isLivingConfigured:{}, sessionsLivings contains {}:{}, session isLiving:{}",
				isLivingConfigured(), sessionId, this.livingManager.sessionIsBeingLived(sessionId), isLiving.get());
		return isLivingConfigured() && !this.livingManager.sessionIsBeingLived(sessionId)
				&& isLiving.compareAndSet(false, true);
	}

	@Override
	public boolean sessionAllowedToStopLiving() {
		log.info("to stop living, isLivingConfigured:{}, sessionsLivings contains {}:{}, session isLiving:{}",
				isLivingConfigured(), sessionId, this.livingManager.sessionIsBeingLived(sessionId), isLiving.get());
		return isLivingConfigured() && this.livingManager.sessionIsBeingLived(sessionId)
				&& isLiving.compareAndSet(true, false);
	}

	@Override
	public Map<String, String> getLayoutRelativePartId() {
		checkClosed();
		Map<String, String> relativePartIdMap = new HashMap<>();
		this.participants.values().stream()
				.map(v -> v.get(StreamType.MAJOR.name()))
				.forEach(participant -> {
					if (Objects.nonNull(participant)) {
						if (OpenViduRole.MODERATOR.equals(participant.getRole())) {
							relativePartIdMap.put("moderatorId", participant.getParticipantPublicId());
						}
						if (ParticipantHandStatus.speaker.equals(participant.getHandStatus())) {
							relativePartIdMap.put("speakerId", participant.getParticipantPublicId());
						}
					} else {
						log.info("participants:{}", participants.toString());
					}
				});
		return relativePartIdMap;
	}

	public RecordingManager getRecordingManager() {
		return recordingManager;
	}

	public void setConference(Conference conference){ this.conference = conference; }

	public Conference getConference() { return this.conference; }

	public void setPresetInfo(SessionPreset preset) { this.preset = preset; }

	public SessionPreset getPresetInfo() { return this.preset; }

	public ConferenceModeEnum getConferenceMode() {
		return conferenceMode;
	}

	public void setConferenceMode(ConferenceModeEnum conferenceMode) {
		this.conferenceMode = conferenceMode;
	}

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

    public int getModeratorIndex() {
        return moderatorIndex;
    }

    public void setModeratorIndex(String moderatorPublicId) {
        int length = layoutInfo.size();
        for (int i = 0; i < length; i++) {
            if (layoutInfo.get(i).getAsString().equals(moderatorPublicId)) {
                this.moderatorIndex = i;
                break;
            }
        }
    }

    public void setDelayConfCnt(int delayConfCnt) { this.delayConfCnt = delayConfCnt; }

	public int incDelayConfCnt() { return this.delayConfCnt++; }

	public int getDelayConfCnt() { return this.delayConfCnt; }

	public boolean isDelay() {
		return delay;
	}

	public void setDelay(boolean delay) {
		this.delay = delay;
	}

	public void setNotifyCountdown10Min(boolean notifyCountdown10Min) { this.notifyCountdown10Min = notifyCountdown10Min; }

	public boolean getNotifyCountdown10Min() { return this.notifyCountdown10Min; }

	public void setNotifyCountdown1Min(boolean notifyCountdown1Min) { this.notifyCountdown1Min = notifyCountdown1Min; }

	public boolean getNotifyCountdown1Min() { return this.notifyCountdown1Min; }


	public Long getStartRecordingTime() {
		return startRecordingTime;
	}

	public void setStartRecordingTime(Long startRecordingTime) {
		this.startRecordingTime = startRecordingTime;
	}

	public Long getStopRecordingTime() {
		return stopRecordingTime;
	}

	public void setStopRecordingTime(Long stopRecordingTime) {
		this.stopRecordingTime = stopRecordingTime;
	}

	public Long getStartLivingTime() {
		return startLivingTime;
	}

	public void setStartLivingTime(Long startLivingTime) {
		this.startLivingTime = startLivingTime;
	}

	public String getLivingUrl() {
		return livingUrl;
	}

	public void setLivingUrl(String livingUrl) {
		this.livingUrl = livingUrl;
	}

	public int getConfDelayTime() { return this.delayConfCnt * this.delayTimeUnit; }

	public long getConfStartTime() {						// unit is ms
		return getConference().getStartTime().getTime();
	}

	public long getConfEndTime() {// unit is ms
		if (getPresetInfo().getRoomDuration() == -1) {
			return 0;
		}
        int confDuration = Float.valueOf(getPresetInfo().getRoomDuration() * 60 * 60 * 1000).intValue() + getConfDelayTime() * 1000;
        return getConfStartTime() + confDuration;

	}

	public long getConfRemainTime() {						// unit is second
		return (getConfEndTime() - new Date().getTime()) / 1000;
	}

	/**
	 * 会议人数
	 */
	public int getPartSize() {
		return this.participantList.size();
	}

    public Set<Participant> getParticipants() {
        checkClosed();
        return new LinkedHashSet<>(this.participantList.values());
    }

    @Deprecated // use getParticipants()
	public Set<Participant> getMajorPartEachConnect() {
		return getParticipants();
	}

	public Set<Participant> getMajorSipPart() {
		checkClosed();
		return this.participants.values().stream()
				.map(v -> v.get(StreamType.MAJOR.name()))
				.filter(participant -> participant.getTerminalType()==TerminalTypeEnum.S)
				.collect(Collectors.toSet());
	}

	public Set<Participant> getMajorPartExcludeModeratorConnect() {
		checkClosed();
		return this.participantList.values().stream()
				.filter(participant -> Objects.nonNull(participant) && !Objects.equals(OpenViduRole.MODERATOR, participant.getRole()))
				.collect(Collectors.toSet());
	}

	public Set<Participant> getMajorPartAllOrSpecificConnect(String userUuid) {
		checkClosed();
		return org.apache.commons.lang.StringUtils.isEmpty(userUuid) ? this.participants.values().stream()
				.map(v -> v.get(StreamType.MAJOR.name()))
				.filter(participant -> Objects.nonNull(participant)
						&& !Objects.equals(OpenViduRole.THOR, participant.getRole()))
				.collect(Collectors.toSet()) : this.participants.values().stream()
				.map(v -> v.get(StreamType.MAJOR.name()))
				.filter(participant -> Objects.nonNull(participant) && userUuid.equals(participant.getUuid())
						&& !Objects.equals(OpenViduRole.THOR, participant.getRole()))
				.collect(Collectors.toSet());
	}

	public Set<Participant> getMajorPartEachIncludeThorConnect() {
		checkClosed();
		return this.participants.values().stream()
				.map(v -> v.get(StreamType.MAJOR.name()))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

//	public Set<Participant> getMajorPartEachExcludeThorConnect() {
//		checkClosed();
//		return this.participants.values().stream().map(v -> v.get(StreamType.MAJOR.name()))
//				.filter(participant -> Objects.nonNull(participant)
//						&& !Objects.equals(OpenViduRole.THOR, participant.getRole()))
//				.collect(Collectors.toSet());
//	}

	public Set<Participant> getMajorAndMinorPartEachConnect() {
		checkClosed();
		return this.participants.values().stream()
				.flatMap(v -> v.values().stream())
				.filter(participant -> !OpenViduRole.THOR.equals(participant.getRole()))
				.collect(Collectors.toSet());
	}

	public Set<Participant> getPartsExcludeModeratorAndSpeaker() {
		checkClosed();
		return this.participants.values().stream().map(v -> v.get(StreamType.MAJOR.name()))
				.filter(participant -> Objects.nonNull(participant)
						&& !Objects.equals(OpenViduRole.THOR, participant.getRole())
						&& !Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())
						&& !Objects.equals(OpenViduRole.MODERATOR, participant.getRole())
						&& !Objects.equals(OpenViduRole.ONLY_SHARE, participant.getRole()))
				.collect(Collectors.toSet());
	}

    public List<Participant> getOrderedMajorAndOnWallParts() {
        checkClosed();
        return this.participants.values().stream()
                .map(v -> v.get(StreamType.MAJOR.name()))
                .filter(participant -> Objects.nonNull(participant) && participant.getRole().needToPublish()
						&& participant.isStreaming())
				.sorted(Comparator.comparing(Participant::getOrder))
				.collect(Collectors.toList());
	}

	public Participant getParticipantByPrivateId(String participantPrivateId) {
		checkClosed();
//		if (Objects.isNull(participants.get(participantPrivateId))) {
//			return null;
//		}
//
//        return participants.get(participantPrivateId).get(StreamType.MAJOR.name());
		return participantList.values().stream().filter(p -> p.getUuid().equals(participantPrivateId)).findFirst().orElseGet(null);
	}

	public Optional<Participant> getParticipantByPrivateId(String participantPrivateId, String uuid) {
		checkClosed();
		Participant participant = participantList.get(uuid);
		if (participant == null) {
			return Optional.empty();
		}
		if (participant.getParticipantPrivateId().equals(participantPrivateId)) {
			return Optional.of(participant);
		}
		return Optional.empty();
	}

	@Override
	public ConcurrentMap<String, Participant> getSamePrivateIdParts(String participantPrivateId) {
		return participants.get(participantPrivateId);
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
	//todo 2.0 Deprecated
	@Deprecated
	public Participant getParticipantByPublicId(String participantPublicId) {
		checkClosed();
		for (Participant p : getParticipants()) {
			if (p.getParticipantPublicId().equals(participantPublicId)) {
				return p;
			}
		}
		return null;
	}

	//todo 2.0 Deprecated
	@Deprecated
	@Override
	public Map<String, Participant> getSameAccountParticipants(String userUuid) {
		checkClosed();
		return this.participants.values().stream()
				.flatMap(v -> v.values().stream())
				.filter(participant -> userUuid.equals(participant.getUuid())
						&& !OpenViduRole.THOR.equals(participant.getRole()))
				.collect(Collectors.toMap(v -> v.getStreamType().name(), Function.identity()));
	}

	public Participant getParticipantByUserId(Long userId) {
    	checkClosed();
		return this.participants.values().stream().map(v -> v.get(StreamType.MAJOR.name()))
				.filter(participant -> Objects.nonNull(participant) && userId.equals(participant.getUserId())
						&& !participant.getRole().equals(OpenViduRole.THOR)).findAny().orElse(null);
	}

	public Optional<Participant> getParticipantByUUID(String uuid) {
		checkClosed();
		return Optional.ofNullable(participantList.get(uuid));
	}

	//todo 2.0 delete
//	public Participant getSpeakerPart() {
//		checkClosed();
//		return this.participants.values().stream().map(v -> v.get(StreamType.MAJOR.name()))
//				.filter(participant -> Objects.nonNull(participant) && Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus())
//						&& !participant.getRole().equals(OpenViduRole.THOR)).findAny().orElse(null);
//	}

	public Participant getModeratorPart() {
		checkClosed();
		return this.participantList.values().stream().filter(p -> p.getRole() == OpenViduRole.MODERATOR).findFirst().orElseGet(null);
	}

	public List<Participant> getModeratorAndThorPart() {
		checkClosed();
		return this.participants.values().stream().map(v -> v.get(StreamType.MAJOR.name()))
				.filter(participant -> Objects.nonNull(participant) && participant.getRole().isController()).collect(Collectors.toList());
	}
	//todo 2.0 Deprecated
	@Deprecated
	public Participant getThorPart() {
		checkClosed();
		return this.participants.values().stream().map(v -> v.get(StreamType.MAJOR.name()))
				.filter(participant -> Objects.nonNull(participant) && participant.getRole().equals(OpenViduRole.THOR)).findAny().orElse(null);
	}
		//todo 2.0 delete
//	public Participant getSharingPart() {
//		checkClosed();
//		return this.participants.values().stream().map(v -> v.get(StreamType.SHARING.name())).filter(Objects::nonNull).findAny().orElse(null);
//	}

    public Participant getParticipantByStreamId(String streamId) {
        checkClosed();
        return this.participants.values().stream().flatMap(v -> v.values().stream()).filter(participant ->
                participant.isStreaming() && streamId.equals(participant.getPublisherStreamId())).findAny().orElse(null);
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

	public boolean needToChangePartRoleAccordingToLimit(Participant participant) {
    	if (StreamType.MAJOR.equals(participant.getStreamType()) && !OpenViduRole.THOR.equals(participant.getRole())) {
    		int size = participantList.size();
			log.info("ParticipantName:{} join session:{} and after increment majorPart size:{}",
					participant.getParticipantName(), sessionId, size);

			return size > openviduConfig.getMcuMajorPartLimit();
		}
    	return false;
	}

	public void setMajorPartsOrder(Participant participant, RpcNotificationService rpcNotificationService) {
		int order;
		boolean cutInLine = false;

		order = roomOrder.incrementAndGet();
		if (OpenViduRole.MODERATOR.equals(participant.getRole())) {
			order = 0;
			cutInLine = true;
		} else if (participant.getOrder() != 0) {//重连 //todo 2.0 重连不需要order变化
			order = participant.getOrder();
			cutInLine = true;
		}
		participant.setOrder(order);

		if (cutInLine) {
			Set<Participant> participants = getParticipants();
			if (!CollectionUtils.isEmpty(participants)) {
				for (Participant p : participants) {
					if (p.getOrder() < order) {
						continue;
					}
					p.setOrder(p.getOrder() + 1);
					log.info("moderator reconnect or join again major participant order change, order = {}", p.getOrder());
					// 推送流变订阅流
					if (p.getOrder() == getPresetInfo().getSfuPublisherThreshold() && p.getRole() == OpenViduRole.PUBLISHER
							&& !ParticipantHandStatus.speaker.equals(p.getHandStatus())) {
						log.info("moderator reconnect or join again participant:{} current order:{} and role set {}", p.getUuid(), p.getOrder(), OpenViduRole.SUBSCRIBER);
						p.setRole(OpenViduRole.SUBSCRIBER);
						//downWallNotifyPartOrderOrRoleChanged(p, true, rpcNotificationService);
						notifyPartOrderOrRoleChanged(true, p, true, OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER, rpcNotificationService);
					}
				}
			}
		}

	}




//	public void dealParticipantOrder(Participant leavePart, RpcNotificationService notificationService) {
//		if (StreamType.MAJOR.equals(leavePart.getStreamType()) && !OpenViduRole.THOR.equals(leavePart.getRole())) {
//			if (leavePart.getRole() == OpenViduRole.ONLY_SHARE) {
//				onlyShareOrder.decrementAndGet();
//			} else {
//				roomNormalOrder.decrementAndGet();
//			}
//			log.info("current participant leaveRoom roomParticipants size:{}", this.roomNormalOrder.get() + this.onlyShareOrder.get());
//			dealPartOrderInSessionAfterLeaving(leavePart, notificationService);
//		}
//	}

	public void dealPartOrderInSessionAfterLeaving(Participant leavePart, RpcNotificationService notificationService) {
		Participant sub2PubPart;
		boolean sendPartRoleChanged;
		synchronized (partOrderAdjustLock) {
			roomOrder.decrementAndGet();
			KurentoParticipant kp = (KurentoParticipant) leavePart;
			KurentoSession session = kp.getSession();
			int leavePartOrder = leavePart.getOrder();
			int sfuLimit = session.getPresetInfo().getSfuPublisherThreshold();
			int lineOrder = session.getPresetInfo().getSfuPublisherThreshold() - 1;
			// decrement the part order which original order is bigger than leavePart.
			// recorder the part whose role need to be changed.

			AtomicReference<Participant> sub2PubPartRef = new AtomicReference<>();
			Set<Participant> participants = getParticipants();
			participants.forEach(participant -> {
				log.info("someone:{} order:{} leave and before deal participant:{} order:{}",
						leavePart.getUuid(), leavePartOrder, participant.getUuid(), participant.getOrder());
				int partOrder;
				if ((partOrder = participant.getOrder()) > leavePartOrder) {
					if (leavePartOrder <= lineOrder && partOrder == sfuLimit
							&& OpenViduRole.MODERATOR != participant.getRole()
							&& OpenViduRole.PUBLISHER != participant.getRole()) {    // exclude the moderator and only_share
						sub2PubPartRef.set(participant);
					}
					participant.setOrder(--partOrder);
					// 订阅流变推送流
					if (partOrder < sfuLimit && participant.getRole() == OpenViduRole.SUBSCRIBER) {
						log.info("after order deal and participant:{} current order:{} and role set {}", participant.getUuid(), participant.getOrder(), OpenViduRole.PUBLISHER);
						participant.setRole(OpenViduRole.PUBLISHER);
					}
					log.info("after order deal and participant:{} current order:{}", participant.getUuid(), participant.getOrder());
				}
			});
			sendPartRoleChanged = Objects.nonNull(sub2PubPart = sub2PubPartRef.get());
		}
		// send notification
		notifyPartOrderOrRoleChanged(true ,sub2PubPart, sendPartRoleChanged, OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER, notificationService);
	}

	/**
	 * 现在的位置变化暂时返回完整的order列表，如果比较耗费性能，可以考虑返回只发生变化的
	 *
	 * @param orderChange 是否有发生order变化
	 */
	private void notifyPartOrderOrRoleChanged(boolean orderChange, Participant sub2PubPart, boolean roleChanged,
											  OpenViduRole originalRole, OpenViduRole presentRole,
											  RpcNotificationService notificationService) {
		// get part order info in session
		JsonObject result = new JsonObject();
		JsonArray orderPart = orderChange ? getPartSfuOrderInfo(getParticipants()) : new JsonArray();
		result.add("updateParticipantsOrder", orderPart);
		JsonArray subToPubChangedParam = roleChanged ?
				getPartRoleChangedNotifyParamArr(sub2PubPart, originalRole, presentRole) : new JsonArray();
		result.add("roleChange", subToPubChangedParam);

		result.addProperty("partSize", participants.size());
		result.addProperty("timestamp", System.currentTimeMillis());

		notificationService.sendBatchNotificationConcurrent(getParticipants(), ProtocolElements.PART_ORDER_OR_ROLE_CHANGE_NOTIFY_METHOD, result);
	}

	public JsonArray getPartRoleChangedNotifyParamArr(Participant participant, OpenViduRole originalRole, OpenViduRole presentRole) {
		JsonArray result = new JsonArray(1);
		JsonObject param = getPartRoleChangedNotifyParam(participant, originalRole, presentRole);
		result.add(param);
		return result;
	}


	private JsonArray getPartSfuOrderInfo(Set<Participant> participants) {
		JsonArray partArr = new JsonArray();
		for (Participant participant : participants) {
			JsonObject partObj = new JsonObject();
			partObj.addProperty("uuid", participant.getUuid());
			partObj.addProperty("order", participant.getOrder());

			partArr.add(partObj);
		}
		return partArr;
	}

	public ErrorCodeEnum dealReplaceOrder(Participant source, Participant target, SessionManager sessionManager, Participant operatorPart) {
		Set<Participant> pub2SubPartSet = new HashSet<>();
		JsonArray partRoleChangedArrParam = new JsonArray();

		int sourceOrder = source.getOrder();
		int targetOrder = target.getOrder();

		int sfuPublisherThreshold = getPresetInfo().getSfuPublisherThreshold();

		if (sourceOrder < sfuPublisherThreshold && targetOrder >= sfuPublisherThreshold) {
			if (source.getHandStatus() != ParticipantHandStatus.speaker) {
				source.setRole(OpenViduRole.SUBSCRIBER);
				pub2SubPartSet.add(source);
				partRoleChangedArrParam.add(getPartRoleChangedNotifyParam(source, OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER));
			}
			target.setRole(OpenViduRole.PUBLISHER);
			partRoleChangedArrParam.add(getPartRoleChangedNotifyParam(target, OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER));
		} else if (targetOrder < sfuPublisherThreshold && sourceOrder >= sfuPublisherThreshold) {
			if (target.getHandStatus() != ParticipantHandStatus.speaker) {
				target.setRole(OpenViduRole.SUBSCRIBER);
				pub2SubPartSet.add(target);
				partRoleChangedArrParam.add(getPartRoleChangedNotifyParam(target, OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER));
			}
			source.setRole(OpenViduRole.PUBLISHER);
			partRoleChangedArrParam.add(getPartRoleChangedNotifyParam(source, OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER));
		}
		source.setOrder(targetOrder);
		target.setOrder(sourceOrder);

		JsonObject result = new JsonObject();
		result.add("updateParticipantsOrder", getPartSfuOrderInfo(Sets.newHashSet(target, source)));
		result.add("roleChange", partRoleChangedArrParam);
		result.addProperty("partSize", getParticipants().size());
		result.addProperty("timestamp", System.currentTimeMillis());
		result.addProperty("source", source.getUuid());
		result.addProperty("target", target.getUuid());
		result.addProperty("operator", operatorPart.getUuid());
		result.addProperty("roomId", operatorPart.getSessionId());
		sessionManager.notificationService.sendBatchNotificationConcurrent(getParticipants(), ProtocolElements.REPLACE_PARTICIPANTS_ORDER_NOTIFY_METHOD, result);

		pub2SubPartSet.forEach(pub2SubPart -> {
			doPubToSub(pub2SubPart, operatorPart, sessionManager);
		});

		return ErrorCodeEnum.SUCCESS;
	}

	public ErrorCodeEnum dealPartOrderAfterRoleChanged(Map<String, Integer> partOrderMap, SessionManager sessionManager,Participant operatorPart) {
		ErrorCodeEnum errorCodeEnum = ErrorCodeEnum.SUCCESS;
		int lineOrder = getPresetInfo().getSfuPublisherThreshold() - 1;
		RpcNotificationService notificationService = sessionManager.notificationService;

		Set<Participant> sub2PubPartSet = new HashSet<>();
		Set<Participant> pub2SubPartSet = new HashSet<>();
		boolean executeFlag = false;

		synchronized (partOrderAdjustLock) {
			for (Map.Entry<String, Integer> map : partOrderMap.entrySet()) {
				String uuid = map.getKey();
				Integer newOrder = map.getValue();
				Optional<Participant> partOp = getParticipantByUUID(uuid);
				if (!partOp.isPresent()) {
					log.info("updateParticipantsOrder participant:{} is null", uuid);
					continue;
				}

				Participant part = partOp.get();
				if (newOrder == part.getOrder()) {
					log.info(" updateParticipantsOrder participant:{},oldOrder:{},newOrder:{}", uuid, part.getOrder(), newOrder);
					continue;
				}

				part.setOrder(newOrder);
				getParticipants().forEach(otherPart -> {
					if (otherPart.getUuid().equals(part.getUuid())) {
						return;
					}
					if (otherPart.getOrder() >= newOrder) {
						otherPart.setOrder(otherPart.getOrder() + 1);
					}
				});
			}

			// 将断断续续的order变连续的
			List<Participant> participants = getParticipants().stream().sorted(Comparator.comparing(Participant::getOrder))
					.collect(Collectors.toList());
			for (int i = 0; i < participants.size(); i++) {
				participants.get(i).setOrder(i);
			}

			int sfuLimit = getPresetInfo().getSfuPublisherThreshold();
			participants.forEach(participant -> {
				try {
					int order = participant.getOrder();
					if (order < sfuLimit && participant.getRole() == OpenViduRole.SUBSCRIBER) {
						sub2PubPartSet.add(participant);
					} else if (order >= sfuLimit && participant.getRole() != OpenViduRole.SUBSCRIBER
							&& participant.getHandStatus() != ParticipantHandStatus.speaker) {
						pub2SubPartSet.add(participant);
					}
				} catch (Exception e) {
					log.error("dealPartOrderAfterRoleChanged error partOrderMap = {}", partOrderMap.toString(), e);
				}
			});

			// change role and construct notify param
			JsonArray partRoleChangedArrParam = new JsonArray();
			sub2PubPartSet.forEach(sub2PubPart -> {
				sub2PubPart.changePartRole(OpenViduRole.PUBLISHER);
				partRoleChangedArrParam.add(getPartRoleChangedNotifyParam(sub2PubPart, OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER));

				// update participant cache info
				updatePartCacheInfo(sub2PubPart);
			});

			pub2SubPartSet.forEach(pub2SubPart -> {
				pub2SubPart.changePartRole(OpenViduRole.SUBSCRIBER);
				partRoleChangedArrParam.add(getPartRoleChangedNotifyParam(pub2SubPart, OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER));

				// update participant cache info
				updatePartCacheInfo(pub2SubPart);
			});

			// send notify
			// get part order info in session
			JsonObject result = new JsonObject();
			JsonArray orderPart = getPartSfuOrderInfo(getParticipants());
			result.add("updateParticipantsOrder", orderPart);
			result.add("roleChange", partRoleChangedArrParam);
			result.addProperty("partSize", participants.size());
			result.addProperty("timestamp", System.currentTimeMillis());
			notificationService.sendBatchNotificationConcurrent(getParticipants(), ProtocolElements.PART_ORDER_OR_ROLE_CHANGE_NOTIFY_METHOD, result);

			pub2SubPartSet.forEach(pub2SubPart -> {
				doPubToSub(pub2SubPart, operatorPart, sessionManager);
			});

		}
		return errorCodeEnum;
	}

    /**
     * 角色从发布者 变成 订阅者
     */
    private void doPubToSub(Participant participant, Participant operatorPart, SessionManager sessionManager) {

        if (ParticipantMicStatus.on.equals(participant.getMicStatus())) {
            participant.setMicStatus(ParticipantMicStatus.off);
            JsonObject audioParams = new JsonObject();
            JsonArray targetIds = new JsonArray();
            targetIds.add(participant.getUuid());
            audioParams.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, operatorPart.getSessionId());
            audioParams.addProperty(ProtocolElements.SET_AUDIO_SOURCE_PARAM, operatorPart.getUuid());
            audioParams.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, "off");
            audioParams.add(ProtocolElements.SET_VIDEO_TARGETS_PARAM, targetIds);

            sessionManager.notificationService.sendBatchNotificationConcurrent(getParticipants(),
                    ProtocolElements.SET_AUDIO_STATUS_METHOD, audioParams);
        }

        // 关闭分享
		if (isShare(participant.getUuid())) {
			sessionManager.endSharing(this, participant, operatorPart.getUuid());
		}

        // TODO 2.0 吴冰 关闭发言
    }

	private void updatePartCacheInfo(Participant participant) {
		Map<String, Object> updateMap = new HashMap<>();
		updateMap.put("role", participant.getRole().name());
		updateMap.put("order", participant.getOrder());
		openviduConfig.getCacheManage().batchUpdatePartInfo(participant.getUuid(), updateMap);
	}

	public boolean isClosed() {
		return closed;
	}

	protected void checkClosed() {
		if (isClosed()) {
			throw new OpenViduException(Code.ROOM_CLOSED_ERROR_CODE, "The session '" + sessionId + "' is closed");
		}
	}

	public void setClosing(boolean closing) {
    	log.info("set session:{} status ===> closing", sessionId);
		this.closing = closing;
	}

	public boolean isClosing() {
		return closing;
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
		json.addProperty("ruid", this.ruid);
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

		if (this instanceof KurentoSession) {
			KurentoSession ks = (KurentoSession) this;

			JsonObject kmsJson = new JsonObject();
			kmsJson.addProperty("kmsId", ks.getKms().getId());
			kmsJson.addProperty("kmsIp", ks.getKms().getIp());
			kmsJson.addProperty("kmsUri", ks.getKms().getUri());
			kmsJson.addProperty("usedMemory", ks.getKms().getKurentoClient().getServerManager().getUsedMemory() / 1024);
			json.add("kms", kmsJson);

			if (!ks.getDeliveryKmsManagers().isEmpty()) {
				JsonArray dkms = new JsonArray();
				kmsJson.add("deliveryKmss", dkms);
				for (DeliveryKmsManager dkm : ks.getDeliveryKmsManagers()) {
					JsonObject kmsj = new JsonObject();
					kmsj.addProperty("kmsId", dkm.getKms().getId());
					kmsj.addProperty("kmsIp", dkm.getKms().getIp());
					kmsj.addProperty("kmsUri", dkm.getKms().getUri());
					kmsj.addProperty("usedMemory", dkm.getKms().getKurentoClient().getServerManager().getUsedMemory() / 1024);
					dkms.add(kmsj);
				}
			}
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

	/**
	 * send conferenceLayoutChanged notify to clients
	 * change the role of original part which is publishing from PUBLISHER to SUBSCRIBER
	 * evict the lastPart according to its role and status(speaker/sharing)
	 * change the subscriberPart role from SUBSCRIBER to PUBLISHER
	 * send notifyPartRoleChanged notify to clients
	 */
	public ErrorCodeEnum evictPartInCompositeWhenSubToPublish(Participant subscriberPart, SessionManager sessionManager) {
		// get the last participant in session's majorShareMixLinkedArr
		JsonObject lastPartObj = majorShareMixLinkedArr.get(majorShareMixLinkedArr.size() - 1).getAsJsonObject();
		Participant lastPart = getParticipantByPublicId(lastPartObj.get("connectionId").getAsString());
        if (OpenViduRole.MODERATOR.equals(lastPart.getRole())) {
            if (majorShareMixLinkedArr.size() == 1) {
                // moderator can not be replaced in session
                return ErrorCodeEnum.INVALID_METHOD_CALL;
            } else {
                lastPartObj = majorShareMixLinkedArr.get(majorShareMixLinkedArr.size() - 2).getAsJsonObject();
                lastPart = getParticipantByPublicId(lastPartObj.get("connectionId").getAsString());
            }
        }
        dealUpAndDownTheWall(lastPart, subscriberPart, sessionManager, true);

		return ErrorCodeEnum.SUCCESS;
	}

	public void dealUpAndDownTheWall(Participant pup2SubPart, Participant sub2PubPart, SessionManager sessionManager, boolean isSub2PubSpeaker) {
        Set<Participant> participants = getParticipants();
        Participant otherPart = getPartByPrivateIdAndStreamType(pup2SubPart.getParticipantPrivateId(),
                StreamType.MAJOR.equals(pup2SubPart.getStreamType()) ? StreamType.SHARING : StreamType.MAJOR);

        if (ParticipantHandStatus.speaker.equals(pup2SubPart.getHandStatus())) {
            // send endRoll notify
            JsonObject params = new JsonObject();
            params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, sessionId);
            params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, pup2SubPart.getUserId().toString());
            participants.forEach(part -> {
                if (Objects.equals(StreamType.MAJOR, part.getStreamType())) {
                    sessionManager.notificationService.sendNotification(part.getParticipantPrivateId(),
                            ProtocolElements.END_ROLL_CALL_METHOD, params);
                }
            });
            pup2SubPart.changeHandStatus(ParticipantHandStatus.endSpeaker);
        }

        // change lastPart role
		changeThePartRole(sessionManager, pup2SubPart, OpenViduRole.PUBLISHER, OpenViduRole.SUBSCRIBER, false);

        // evict the parts in session and notify KMS layout changed
        Participant moderatorPart = getModeratorPart();
        sessionManager.unpublishStream(this, pup2SubPart.getPublisherStreamId(), moderatorPart,
                null, EndReason.forceUnpublishByUser);

        boolean sendStopShareNotify = false;
		JsonObject stopShareParams = new JsonObject();
        if (Objects.nonNull(otherPart)) {
            sessionManager.unpublishStream(this, otherPart.getPublisherStreamId(), moderatorPart,
                    null, EndReason.forceUnpublishByUser);
            sendStopShareNotify = true;
			stopShareParams.addProperty(ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_CONNECTIONID_PARAM,
					StreamType.SHARING.equals(otherPart.getStreamType()) ?
							otherPart.getParticipantPublicId() : pup2SubPart.getParticipantPublicId());
        }

        // send conferenceLayoutChanged notify
		for (Participant participant : participants) {
			if (StreamType.MAJOR.equals(participant.getStreamType())) {
				sessionManager.notificationService.sendNotification(participant.getParticipantPrivateId(),
						ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY, getLayoutNotifyInfo());
				if (!sendStopShareNotify) {
					continue;
				}
				sessionManager.notificationService.sendNotification(participant.getParticipantPrivateId(),
						ProtocolElements.RECONNECTPART_STOP_PUBLISH_SHARING_METHOD, stopShareParams);
			}
		}

		// change subscriberPart role
		changeThePartRole(sessionManager, sub2PubPart, OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER, isSub2PubSpeaker);
    }

	private void changeThePartRole(SessionManager sessionManager, Participant partChanged,
								   OpenViduRole originalRole, OpenViduRole presentRole, boolean isSub2PubSpeaker) {
		partChanged.changePartRole(presentRole);
		JsonObject notifyParam = getPartRoleChangedNotifyParam(partChanged, originalRole, presentRole);
		if (OpenViduRole.PUBLISHER.equals(presentRole)) {
			KurentoParticipant kurentoParticipant = (KurentoParticipant) partChanged;
			kurentoParticipant.createPublisher();
			if (isSub2PubSpeaker) {
				notifyParam.addProperty(ProtocolElements.NOTIFY_PART_ROLE_CHANGED_HAND_STATUS_PARAM, ParticipantHandStatus.speaker.name());
			}
		}
		getParticipants().forEach(participant -> {
			if (StreamType.MAJOR.equals(participant.getStreamType())) {
				sessionManager.notificationService.sendNotification(participant.getParticipantPrivateId(),
						ProtocolElements.NOTIFY_PART_ROLE_CHANGED_METHOD, notifyParam);
			}
		});
	}

	private JsonObject getPartRoleChangedNotifyParam(Participant participant, OpenViduRole originalRole, OpenViduRole presentRole) {
		JsonObject param = new JsonObject();
		param.addProperty(ProtocolElements.NOTIFY_PART_ROLE_CHANGED_UUID_PARAM, participant.getUuid());
		param.addProperty(ProtocolElements.NOTIFY_PART_ROLE_CHANGED_ORIGINAL_ROLE_PARAM, originalRole.name());
		param.addProperty(ProtocolElements.NOTIFY_PART_ROLE_CHANGED_PRESENT_ROLE_PARAM, presentRole.name());
		return param;
	}

	// TODO record the order when part publish and put the first order part which down the wall
	// current version put the random participant who down the wall
	public void putPartOnWallAutomatically(SessionManager sessionManager) {
		if (closing) {
			log.info("session:{} is closing, no need to putPartOnWallAutomatically.", sessionId);
			return;
		}
		if (ConferenceModeEnum.MCU.equals(getConferenceMode()) && participantList.size() >= openviduConfig.getMcuMajorPartLimit()) {
			List<String> publishedParts = new ArrayList<>(16);
			for (JsonElement jsonElement : majorShareMixLinkedArr) {
				publishedParts.add(jsonElement.getAsJsonObject().get("connectionId").getAsString());
			}
			Set<Participant> participants = getMajorPartEachConnect();
			Participant automaticOnWallPart = participants.stream()
					.filter(participant -> !publishedParts.contains(participant.getParticipantPublicId())
							&& OpenViduRole.SUBSCRIBER.equals(participant.getRole())
							&& StreamType.MAJOR.equals(participant.getStreamType()))
					.findAny().orElse(null);
			if (Objects.nonNull(automaticOnWallPart)) {
			    log.info("Put Part:{} On Wall Automatically in session:{}", automaticOnWallPart.getParticipantName(), automaticOnWallPart.getSessionId());
				changeThePartRole(sessionManager, automaticOnWallPart, OpenViduRole.SUBSCRIBER, OpenViduRole.PUBLISHER, false);
			} else {
				log.info("Not found the below wall SUBSCRIBER participant.");
			}
		} else {
			log.info("Not above the conditions when put Part On Wall Automatically.");
		}
	}

	public JsonArray getMajorShareMixLinkedArr() {
		return majorShareMixLinkedArr;
	}

	public synchronized void dealParticipantDefaultOrder(KurentoParticipant kurentoParticipant, EndpointTypeEnum... typeEnums) {
    	if (majorShareMixLinkedArr.size() == layoutCoordinates.size()) {
    		boolean notContains = true;
    		for (JsonElement jsonElement : majorShareMixLinkedArr) {
    			if (jsonElement.getAsJsonObject().get("connectionId").getAsString().equals(kurentoParticipant.getParticipantPublicId())) {
    				notContains = false;
    				break;
				}
			}
    		if (notContains && automatically && layoutMode.ordinal() < (LayoutModeEnum.values().length - 1)) {
				// switch layout mode automatically
				switchLayoutMode(LayoutModeEnum.values()[layoutMode.ordinal() + 1]);
			}
		}

		if (kurentoParticipant.getRole().equals(OpenViduRole.MODERATOR)) {
			if (Objects.isNull(getSharingPart()) && !getSpeakerPart().isPresent()) {
				majorShareMixLinkedArr = reorderIfPriorityJoined(StreamType.MAJOR, kurentoParticipant.getParticipantPublicId());
			} else {
				JsonObject newPart = getPartOrderInfo(StreamType.MAJOR.name(), kurentoParticipant.getParticipantPublicId());
				if (!majorShareMixLinkedArr.contains(newPart)) {
					majorShareMixLinkedArr.add(newPart);
				}
			}
		} else if (Objects.equals(StreamType.SHARING, kurentoParticipant.getStreamType())) {
			majorShareMixLinkedArr = reorderIfPriorityJoined(StreamType.SHARING, kurentoParticipant.getParticipantPublicId());
		} else {
			JsonObject newPart = getPartOrderInfo(StreamType.MAJOR.name(), kurentoParticipant.getParticipantPublicId());
			if (!majorShareMixLinkedArr.contains(newPart)) {
				majorShareMixLinkedArr.add(newPart);
			}
		}

    	log.info("dealParticipantDefaultOrder majorShareMixLinkedArr:{}", majorShareMixLinkedArr.toString());
    	this.invokeKmsConferenceLayout(typeEnums);
	}

	private static JsonObject getPartOrderInfo(String streamType, String publicId) {
		JsonObject result = new JsonObject();
		result.addProperty("streamType", streamType);
		result.addProperty("connectionId", publicId);
		return result;
	}

	private JsonArray reorderIfPriorityJoined(StreamType streamType, String connectionId) {
		JsonArray newMajorMixLinkedArr = new JsonArray(50);
		JsonObject newPart = getPartOrderInfo(streamType.name(), connectionId);
		if (!majorShareMixLinkedArr.contains(newPart)) {
			newMajorMixLinkedArr.add(newPart);
		}
		newMajorMixLinkedArr.addAll(majorShareMixLinkedArr);
		return newMajorMixLinkedArr;
	}

	public synchronized void reorder(String moderatorPublicId) {
		if (closing) {
			log.info("session:{} is closing, no need to reorder mcu layout.", sessionId);
			return;
		}
    	JsonArray result = new JsonArray(50);
    	JsonArray partExcludeShareAndModerator = new JsonArray(50);
    	JsonObject moderatorObj = null, shareObj = null;
    	for (JsonElement jsonElement : majorShareMixLinkedArr) {
			JsonObject temp = jsonElement.getAsJsonObject();
			if (!StringUtils.isEmpty(moderatorPublicId) &&
                    Objects.equals(moderatorPublicId, temp.get("connectionId").getAsString())) {
				moderatorObj = temp;
			} else if (Objects.equals(StreamType.SHARING.name(), temp.get("streamType").getAsString())) {
				shareObj = temp;
			} else {
				partExcludeShareAndModerator.add(temp);
			}
		}

		if (!Objects.isNull(shareObj)) {
			result.add(shareObj);
		}
    	if (!Objects.isNull(moderatorObj)) {
    		result.add(moderatorObj);
		}
		result.addAll(partExcludeShareAndModerator);
		majorShareMixLinkedArr = result;
	}

	public synchronized boolean leaveRoomSetLayout(Participant participant, String moderatePublicId) {
		if (closing) {
			log.info("session:{} is closing, no need to leaveRoomSetLayout mcu layout.", sessionId);
			return false;
		}
		boolean changed = false;
		for (JsonElement element : majorShareMixLinkedArr) {
			JsonObject jsonObject = element.getAsJsonObject();
			if (Objects.equals(jsonObject.get("connectionId").getAsString(), participant.getParticipantPublicId())) {
				changed = majorShareMixLinkedArr.remove(element);
				break;
			}
		}

		if (!changed) {
			log.info("leaveRoomSetLayout not changed, cause publicId:{} not in majorShareMixLinkedArr:{}",
					participant.getParticipantPublicId(), majorShareMixLinkedArr.toString());
			return false;
		}

		// switch layout mode automatically
		if (automatically && !Objects.equals(LayoutModeEnum.ONE, layoutMode) && majorShareMixLinkedArr.size() <
				layoutMode.getMode()) {
			switchLayoutMode(LayoutModeEnum.values()[layoutMode.ordinal() - 1]);
		}

		if (Objects.equals(ParticipantHandStatus.speaker, participant.getHandStatus()) ||
                Objects.equals(StreamType.SHARING, participant.getStreamType())) {
			reorder(moderatePublicId);
		}

		log.info("leaveRoomSetLayout majorShareMixLinkedArr:{}", majorShareMixLinkedArr.toString());
		return true;
	}

    public synchronized void switchLayoutMode(LayoutModeEnum layoutModeEnum) {
        log.info("session switch layout mode:{} -> {}", layoutMode.getMode(), layoutModeEnum.getMode());
        setLayoutMode(layoutModeEnum);
        setLayoutCoordinates(LayoutInitHandler.getLayoutByMode(layoutModeEnum));
    }

	public synchronized void replacePartOrderInConference(String sourceConnectionId, String targetConnectionId) {
		if (org.apache.commons.lang.StringUtils.isNotEmpty(sourceConnectionId)) {
			boolean existSharing = false;
			for (JsonElement jsonElement : majorShareMixLinkedArr) {
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				String connectionId = jsonObject.get("connectionId").getAsString();
				if (connectionId.equals(sourceConnectionId) || connectionId.equals(targetConnectionId)) {
					if (jsonObject.get("streamType").getAsString().equals(StreamType.SHARING.name())) {
						existSharing = true;
					}
				}
			}

			for (JsonElement jsonElement : majorShareMixLinkedArr) {
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				String connectionId = jsonObject.get("connectionId").getAsString();
				String streamType = jsonObject.get("streamType").getAsString();
				if (connectionId.equals(sourceConnectionId)) {
					changeStreamTypeIfNecessary(jsonObject, targetConnectionId, existSharing, streamType);
				} else if (connectionId.equals(targetConnectionId)) {
					changeStreamTypeIfNecessary(jsonObject, sourceConnectionId, existSharing, streamType);
				}
			}
			log.info("replacePartOrderInConference majorShareMixLinkedArr:{}", majorShareMixLinkedArr.toString());
		}
	}

	private static void changeStreamTypeIfNecessary(JsonObject jsonObject, String connectionId, boolean existSharing, String streamType) {
        jsonObject.addProperty("connectionId", connectionId);
        if (existSharing) {
            jsonObject.addProperty("streamType", streamType.equals(StreamType.SHARING.name()) ? StreamType.MAJOR.name() : StreamType.SHARING.name());
        }
    }

	public synchronized int invokeKmsConferenceLayout(EndpointTypeEnum... typeEnums) {
		if (!closing) {
			KurentoSession kurentoSession = (KurentoSession) this;
			KurentoClient kurentoClient = kurentoSession.getKms().getKurentoClient();
			try {
				kurentoClient.sendJsonRpcRequest(composeLayoutInvokeRequest(kurentoSession.getPipeline().getId(),
						majorShareMixLinkedArr, kurentoClient.getSessionId(), typeEnums));
			} catch (IOException e) {
				log.error("Exception:\n", e);
				return 0;
			}
		} else {
			log.info("session:{} is closing, no need to invokeKmsConferenceLayout mcu layout.", sessionId);
		}

		return 1;
	}


    private Request<JsonObject> composeLayoutInvokeRequest(String pipelineId, JsonArray linkedArr, String sessionId, EndpointTypeEnum... typeEnums) {
		Request<JsonObject> kmsRequest = new Request<>();
		JsonObject params = new JsonObject();
		params.addProperty("object", pipelineId);
		params.addProperty("operation", "setLayout");
		params.addProperty("sessionId", sessionId);
		JsonArray layoutInfos = new JsonArray(50);
		int index = 0;
		int size = linkedArr.size();
		for (JsonElement jsonElement : layoutCoordinates) {
			if (index < size) {
				JsonObject temp = jsonElement.getAsJsonObject().deepCopy();
				try {
					KurentoParticipant kurentoParticipant = (KurentoParticipant) this.getParticipantByPublicId(linkedArr
							.get(index).getAsJsonObject().get("connectionId").getAsString());
					temp.addProperty("connectionId", "connectionId");
					temp.addProperty("streamType", "streamType");

					EndpointTypeEnum type;
					HubPort hubPort = null;
					if (!kurentoParticipant.getSession().getConferenceMode().equals(ConferenceModeEnum.MCU)
							&& Objects.nonNull(typeEnums) && typeEnums.length > 0 && Objects.nonNull(type = typeEnums[0])) {
						hubPort = Objects.equals(type, EndpointTypeEnum.recording) ? kurentoParticipant.getPublisher().getRecordHubPort() : kurentoParticipant.getPublisher().getLiveHubPort();
					}
					temp.addProperty("object", kurentoParticipant.getSession().getConferenceMode().equals(ConferenceModeEnum.MCU) ?
							kurentoParticipant.getPublisher().getMajorShareHubPort().getId() : hubPort.getId());

					temp.addProperty("hasVideo", kurentoParticipant.getPublisherMediaOptions().hasVideo());
					temp.addProperty("onlineStatus", kurentoParticipant.getPublisherMediaOptions().hasVideo() ? "online" : "offline");

					layoutInfos.add(temp);
					index++;
				} catch (Exception e) {
					log.error("Exception when compose layout invoke request:{}, exception:{}", temp.toString(), e);
				}

			} else break;
		}
		JsonObject operationParams = new JsonObject();
		operationParams.add("layoutInfo", layoutInfos);
		params.add("operationParams", operationParams);
		kmsRequest.setMethod("invoke");
		kmsRequest.setParams(params);
		log.info("send sms setLayout params:{}", params);

		return kmsRequest;

	}

	public synchronized void evictReconnectOldPart(String partPublicId) {
    	if (StringUtils.isEmpty(partPublicId)) return;
    	if (Objects.equals(ConferenceModeEnum.MCU, getConferenceMode())) {
			for (JsonElement element : majorShareMixLinkedArr) {
				JsonObject jsonObject = element.getAsJsonObject();
				if (Objects.equals(jsonObject.get("connectionId").getAsString(), partPublicId)) {
					majorShareMixLinkedArr.remove(element);
					if (automatically && !Objects.equals(LayoutModeEnum.ONE, layoutMode)
							&& majorShareMixLinkedArr.size() < layoutMode.getMode()) {
						switchLayoutMode(LayoutModeEnum.values()[layoutMode.ordinal() - 1]);
					}
					break;
				}
			}

			log.info("evictReconnectOldPart majorShareMixLinkedArr:{}", majorShareMixLinkedArr.toString());
		} else {
			for (JsonElement element : layoutInfo) {
				if (Objects.equals(element.getAsString(), partPublicId)) {
                    layoutInfo.remove(element);
                    break;
				}
			}
			if (!Objects.isNull(layoutInfo) && layoutInfo.size() > 0) {
				setLayoutMode(LayoutModeEnum.getLayoutMode(layoutInfo.size()));
			}

			log.info("evictReconnectOldPart layoutInfo:{}", layoutInfo.toString());
		}
	}

	public JsonArray getCurrentPartInMcuLayout() {
    	JsonArray layoutInfos = new JsonArray(50);
    	if (majorShareMixLinkedArr.size() == 0) return layoutInfos;

		int index = 0;
		int size = majorShareMixLinkedArr.size();
		for (JsonElement jsonElement : layoutCoordinates) {
			JsonObject result = jsonElement.getAsJsonObject().deepCopy();
			if (index < size) {
				JsonObject layout = majorShareMixLinkedArr.get(index).getAsJsonObject();
				result.addProperty("connectionId", layout.get("connectionId").getAsString());
				result.addProperty("streamType", layout.get("streamType").getAsString());

				index++;
			}

			layoutInfos.add(result);
		}

		return layoutInfos;
	}

    public JsonObject getLayoutNotifyInfo() {
        JsonObject notifyResult = new JsonObject();
        notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_AUTOMATICALLY_PARAM, this.isAutomatically());
        notifyResult.addProperty(ProtocolElements.CONFERENCELAYOUTCHANGED_NOTIFY_MODE_PARAM, this.getLayoutMode().getMode());
        notifyResult.add(ProtocolElements.CONFERENCELAYOUTCHANGED_PARTLINKEDLIST_PARAM, this.getCurrentPartInMcuLayout());
        return notifyResult;
    }

	public boolean getConferenceRecordStatus() {
		return isRecordingConfigured() && isRecording.get();
	}

	public boolean getLivingStatus() {
		return isLivingConfigured() && this.livingManager.sessionIsBeingLived(sessionId)
				&& isLiving.get();
	}

	public void stopRecordAndLiving(long kmsDisconnectionTime, EndReason reason) {
		// Stop recording if session is being recorded
		if (recordingManager.sessionIsBeingRecorded(sessionId)) {
			this.recordingManager.forceStopRecording(this, reason, kmsDisconnectionTime);
		}

		// Stop living if session is being lived
		if (livingManager.sessionIsBeingLived(sessionId)) {
			this.livingManager.forceStopLiving(this, reason, kmsDisconnectionTime);
		}
	}

	public boolean isModeratorHasMulticastplay() {
		Participant moderatePart = getParticipants().stream().filter(participant ->
				participant.getStreamType().equals(StreamType.MAJOR) && participant.getRole().equals(OpenViduRole.MODERATOR))
				.findAny().orElse(null);
		return Objects.nonNull(moderatePart) && !StringUtils.isEmpty(moderatePart.getAbility())
				&& moderatePart.getAbility().contains(CommonConstants.DEVICE_ABILITY_MULTICASTPALY);
	}

	public boolean ableToUpdateRecord() {
		return isRecording.get() && !closing && !closed;
	}

	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}


	public Optional<Participant> getSharingPart() {
		return Optional.ofNullable(sharingPart);
	}

	public Optional<Participant> getSpeakerPart() {
		return Optional.ofNullable(speakerPart);
	}

	/**
	 * 检查舒服是分享者
	 */
	public boolean isShare(String uuid) {
		return this.sharingPart != null && this.sharingPart.getUuid().equals(uuid);
	}
}
