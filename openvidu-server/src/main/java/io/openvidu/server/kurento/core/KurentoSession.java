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

package io.openvidu.server.kurento.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.LayoutModeEnum;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.layout.LayoutInitHandler;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.endpoint.MediaEndpoint;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.kurento.kms.Kms;
import io.openvidu.server.utils.SafeSleep;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.kurento.client.EventListener;
import org.kurento.client.*;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Pablo Fuente (pablofuenteperez@gmail.com)
 */
public class KurentoSession extends Session {

	private final static Logger log = LoggerFactory.getLogger(Session.class);
	public static final int ASYNC_LATCH_TIMEOUT = 30;
	//public static final int ASYNC_LATCH_TIMEOUT = 60;

	private MediaPipeline pipeline;
	private CountDownLatch pipelineLatch = new CountDownLatch(1);
	private Throwable pipelineCreationErrorCause;

	private Kms kms;

	private List<DeliveryKmsManager> deliveryKmsManagers = new ArrayList<>();

	private KurentoSessionEventsHandler kurentoSessionHandler;
	private KurentoParticipantEndpointConfig kurentoEndpointConfig;

	private final ConcurrentHashMap<String, String> filterStates = new ConcurrentHashMap<>();

	public CompositeService compositeService;

	private Composite sipComposite;

	private Set<String> sipStreamIdSet = new HashSet<>();
	private final ThreadPoolExecutor sipCompositeThreadPoolExes;

	private final Object pipelineCreateLock = new Object();
	private final Object pipelineReleaseLock = new Object();
	private final Object joinOrLeaveLock = new Object();
	private boolean destroyKurentoClient;

	// 服务崩溃或者kill -9等方式非正常关机会导致
	private final Thread leaseThread = new Thread(() -> {
		log.info("room lease thead start,roomId={},ruid={}", sessionId, ruid);
		while (!closed) {
			try {
				kurentoSessionHandler.cacheManage.roomLease(sessionId, ruid);
				TimeUnit.SECONDS.sleep(20);
			} catch (InterruptedException e) {
				return;
			}
		}
		log.info("room lease thead stop,roomId={},ruid={}", sessionId, ruid);
	});

	public final ConcurrentHashMap<String, String> publishedStreamIds = new ConcurrentHashMap<>();

	public KurentoSession(Session sessionNotActive, Kms kms, KurentoSessionEventsHandler kurentoSessionHandler,
						  KurentoParticipantEndpointConfig kurentoEndpointConfig, boolean destroyKurentoClient) {
		super(sessionNotActive);
		this.kms = kms;
		this.destroyKurentoClient = destroyKurentoClient;
		this.kurentoSessionHandler = kurentoSessionHandler;
		this.kurentoEndpointConfig = kurentoEndpointConfig;
		this.compositeService = new CompositeService(sessionNotActive);
		log.info("New SESSION instance with id '{}'", sessionId);
		kurentoSessionHandler.cacheManage.roomLease(sessionId, ruid);
		this.leaseThread.start();
		sipCompositeThreadPoolExes = new ThreadPoolExecutor(0, 1, 10L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(1), new ThreadFactoryBuilder().setNameFormat("sip-composite-thread-" + sessionId + "-%d").setDaemon(true).build()
				, new ThreadPoolExecutor.DiscardPolicy());
	}

	@Override
	public void join(Participant participant) {
		synchronized (joinOrLeaveLock) {
			checkClosed();
			createPipeline();
			if (Objects.equals(getConferenceMode(), ConferenceModeEnum.MCU)) {
				this.compositeService.setPipeline(this.getPipeline());
				compositeService.createMajorShareComposite();
				if (Objects.equals(StreamType.SHARING, participant.getStreamType())) {
					compositeService.setExistSharing(true);
				}
			}
			KurentoParticipant kurentoParticipant = new KurentoParticipant(participant, this, this.kurentoEndpointConfig,
					this.openviduConfig, this.recordingManager, this.livingManager);

			participants.computeIfPresent(participant.getParticipantPrivateId(), (privateId, parts) -> {
				parts.putIfAbsent(participant.getStreamType().name(), kurentoParticipant);
				return parts;
			});

			participants.computeIfAbsent(participant.getParticipantPrivateId(), privateId -> {
				ConcurrentMap<String, Participant> connectionParticipants = new ConcurrentHashMap<>();
				connectionParticipants.put(participant.getStreamType().name(), kurentoParticipant);
				return connectionParticipants;
			});

			filterStates.forEach((filterId, state) -> {
				log.info("Adding filter {}", filterId);
				kurentoSessionHandler.updateFilter(sessionId, participant, filterId, state);
			});

			log.info("SESSION {}: Added participant {}", sessionId, participant);

			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(participant.getParticipantPublicId())) {
				kurentoEndpointConfig.getCdr().recordParticipantJoined(participant, sessionId);
			}
		}
	}

	public void newPublisher(Participant participant) {
		registerPublisher();
		for (Participant p : getParticipants()) {
			if (participant.equals(p)) {
				continue;
			}
			((KurentoParticipant) p).getNewOrExistingSubscriber(participant.getParticipantPublicId());
		}

		log.debug("SESSION {}: Virtually subscribed other participants {} to new publisher {}", sessionId,
				participants.values(), participant.getParticipantPublicId());
	}

	public void cancelPublisher(Participant participant, EndReason reason) {
		for (Participant subscriber : getParticipants()) {
			if (participant.equals(subscriber)) {
				continue;
			}
			((KurentoParticipant) subscriber).cancelReceivingMedia(participant.getParticipantPublicId(), reason);
		}

		log.debug("SESSION {}: Unsubscribed other participants {} from the publisher {}", sessionId,
				participants.values(), participant.getParticipantPublicId());

	}

	@Override
	public void leave(String participantPrivateId, EndReason reason) throws OpenViduException {
		checkClosed();
		for (Participant p : participants.get(participantPrivateId).values()) {
			KurentoParticipant participant = (KurentoParticipant)p;

			if (participant == null) {
				throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE, "Participant with private id "
						+ participantPrivateId + " not found in session '" + sessionId + "'");
			}
			participant.releaseAllFilters();

			log.info("PARTICIPANT {}: Leaving session {}", participant.getParticipantPublicId(), this.sessionId);

			this.removeParticipant(participant, reason);
			participant.close(reason, true, 0);
		}
	}

	@Override
	public void leaveRoom(Participant p, EndReason reason) {
		deregisterMajorParticipant(p);
		synchronized (joinOrLeaveLock) {
			try {
				leave(p, reason);
				log.info("Session:{} participant publicId:{} leave room sleep {}ms", p.getSessionId(),
						p.getParticipantPublicId(), kurentoEndpointConfig.leaveDelay);
				Thread.sleep(kurentoEndpointConfig.leaveDelay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void leave(Participant p, EndReason reason) {
		checkClosed();
		KurentoParticipant participant = (KurentoParticipant)p;
		if (participant == null) {
			throw new OpenViduException(Code.USER_NOT_FOUND_ERROR_CODE, "Participant with private id "
					+ p.getParticipantPrivateId() + "public id " + p.getParticipantPublicId() + " not found in session '" + sessionId + "'");
		}
		participant.releaseAllFilters();

		log.info("PARTICIPANT {}: Leaving session {}", participant.getParticipantPublicId(), this.sessionId);

		this.removeParticipant(participant, reason);
		participant.close(reason, true, 0);
	}

	@Override
	public boolean close(EndReason reason) {
		if (!closed) {
			for (Participant participant : getParticipants()) {
			    KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
                kurentoParticipant.releaseAllFilters();
                kurentoParticipant.close(reason, true, 0);
			}

			participants.clear();
            compositeService.closeMajorShareComposite();
            if (Objects.nonNull(sipComposite)) {
				sipComposite.release();
			}
			closePipeline(null);

			for (DeliveryKmsManager deliveryKmsManager : deliveryKmsManagers) {
				deliveryKmsManager.release();
			}
			deliveryKmsManagers.clear();

			log.debug("Session {} closed", this.sessionId);

			if (destroyKurentoClient) {
				kms.getKurentoClient().destroy();
			}

			// Also disassociate the KurentoSession from the Kms
			kms.removeKurentoSession(this.sessionId);

			this.closed = true;
			return true;
		} else {
			log.warn("Closing an already closed session '{}'", this.sessionId);
			return false;
		}
	}

	public void sendIceCandidate(String participantPrivateId, String senderPublicId, String endpointName,
			IceCandidate candidate) {
		this.kurentoSessionHandler.onIceCandidate(sessionId, participantPrivateId, senderPublicId, endpointName,
				candidate);
	}

	public void sendMediaError(String participantId, String description) {
		this.kurentoSessionHandler.onMediaElementError(sessionId, participantId, description);
	}

	private void removeParticipant(Participant participant, EndReason reason) {

		checkClosed();

		ConcurrentMap majorOrSharePartsMap = participants.get(participant.getParticipantPrivateId());
		if (Objects.nonNull(majorOrSharePartsMap)) {
			majorOrSharePartsMap.remove(participant.getStreamType().name());
			if (majorOrSharePartsMap.size() == 0) {
				participants.remove(participant.getParticipantPrivateId());
			}
		}
		/*Participant p1 = participants.get(participant.getParticipantPrivateId()).remove(participant.getStreamType().name());
		if (participants.get(participant.getParticipantPrivateId()).size() == 0) {
			participants.remove(participant.getParticipantPrivateId());
		}*/

		log.debug("SESSION {}: Cancel receiving media from participant '{}' for other participant", this.sessionId,
				participant.getParticipantPublicId());
        for (Participant other : getParticipants()) {
			((KurentoParticipant) other).cancelReceivingMedia(participant.getParticipantPublicId(), reason);
		}
	}

	public Kms getKms() {
		return this.kms;
	}

	public MediaPipeline getPipeline() {
		try {
			pipelineLatch.await(KurentoSession.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return this.pipeline;
	}

    private void createPipeline() {
		if (pipeline != null) {
			return;
		}
		synchronized (pipelineCreateLock) {
			if (pipeline != null) {
				return;
			}
			log.info("SESSION {}: Creating MediaPipeline", sessionId);
			try {
				kms.getKurentoClient().createMediaPipeline(new Continuation<MediaPipeline>() {
					@Override
					public void onSuccess(MediaPipeline result) throws Exception {
						pipeline = result;
						pipelineLatch.countDown();
						pipeline.setName(sessionId);
						log.debug("SESSION {}: Created MediaPipeline", sessionId);
					}

					@Override
					public void onError(Throwable cause) throws Exception {
						pipelineCreationErrorCause = cause;
						pipelineLatch.countDown();
						log.error("SESSION {}: Failed to create MediaPipeline", sessionId, cause);
					}
				});
			} catch (Exception e) {
				log.error("Unable to create media pipeline for session '{}'", sessionId, e);
				pipelineLatch.countDown();
			}
			if (getPipeline() == null) {
				final String message = pipelineCreationErrorCause != null
						? pipelineCreationErrorCause.getLocalizedMessage()
						: "Unable to create media pipeline for session '" + sessionId + "'";
				pipelineCreationErrorCause = null;
				throw new OpenViduException(Code.ROOM_CANNOT_BE_CREATED_ERROR_CODE, message);
			}

			pipeline.addErrorListener(new EventListener<ErrorEvent>() {
				@Override
				public void onEvent(ErrorEvent event) {
					String desc = event.getType() + ": " + event.getDescription() + "(errCode=" + event.getErrorCode()
							+ ")";
					log.warn("SESSION {}: Pipeline error encountered: {}", sessionId, desc);
					kurentoSessionHandler.onPipelineError(sessionId, getParticipants(), desc);
				}
			});
		}
	}

	public Composite createSipComposite() {
		if (Objects.isNull(sipComposite)) {
			sipComposite = new Composite.Builder(pipeline).build();
			sipComposite.setName(sessionId + "-sipComposite-" + RandomStringUtils.randomAlphabetic(5));
			log.info("create sip composite {}, name {}", sipComposite.getId(), sipComposite.getName());
		}
		return sipComposite;
	}

	public Composite getSipComposite() {
		return sipComposite;
	}

	/**
	 * 对比sip中的辅流是否有发生改变，如果有则返回true
	 */
	public boolean equalsSipCompositeStream(Participant... participants) {
		Set<String> newStreamSet = Arrays.stream(participants).filter(Objects::nonNull)
				.map(p -> ((KurentoParticipant) p).getPublisher()).filter(Objects::nonNull)
				.map(MediaEndpoint::getStreamId).collect(Collectors.toSet());
		boolean equalCollection = CollectionUtils.isEqualCollection(newStreamSet, sipStreamIdSet);
		log.info("oldSipStreamIdSet {}, newSipStreamIdSet {}", sipStreamIdSet, newStreamSet);
		if (!equalCollection) {
			sipStreamIdSet = newStreamSet;
		}
		return equalCollection;
	}

	public void asyncUpdateSipComposite() {
		if (Objects.isNull(getSipComposite())){
			return;
		}
		sipCompositeThreadPoolExes.execute(this::updateSipComposite);
	}

	private void updateSipComposite() {
		SafeSleep.sleepMilliSeconds(200);
		int mcuNum = 0;
		JsonArray hubPortIds = new JsonArray(3);
		Participant moderator = null, sharing = null, speaker = null;
		Set<Participant> participants = getParticipants();
		KurentoSession kurentoSession = (KurentoSession) this;
		if (!CollectionUtils.isEmpty(participants)) {
			for (Participant participant : participants) {
				if (OpenViduRole.MODERATOR == participant.getRole() && StreamType.MAJOR == participant.getStreamType()) {
					moderator = participant;
//					Participant minorModerator = getPartByPrivateIdAndStreamType(moderator.getParticipantPrivateId(), StreamType.MINOR);
//					moderator = Optional.ofNullable(minorModerator).orElse(moderator);
				}
				if (StreamType.SHARING == participant.getStreamType()) {
					sharing = participant;
				}
				if (StreamType.MAJOR == participant.getStreamType() && ParticipantHandStatus.speaker == participant.getHandStatus()) {
					speaker = participant;
				}
			}

			if (kurentoSession.equalsSipCompositeStream(moderator, sharing, speaker)) {
				log.info("sip composite stream list no change");
				return;
			}

			// set composite order
			try {
				if (Objects.nonNull(sharing)) {
					log.info("sip found sharing");
					mcuNum = getSipCompositeElements(kurentoSession, sharing, hubPortIds, mcuNum);
				}
				if (Objects.nonNull(speaker)) {
					log.info("sip found speaker");
					mcuNum = getSipCompositeElements(kurentoSession, speaker, hubPortIds, mcuNum);
				}
				if (Objects.nonNull(moderator)) {
					log.info("sip found moderator");
					mcuNum = getSipCompositeElements(kurentoSession, moderator, hubPortIds, mcuNum);
				}
				log.info("sip MCU composite number:{} and composite hub port ids:{}", mcuNum, hubPortIds.toString());
				if (mcuNum > 0) {
					try {
						kurentoSession.getKms().getKurentoClient()
								.sendJsonRpcRequest(composeLayoutRequestForSip(kurentoSession.getPipeline().getId(),
										sessionId, hubPortIds, LayoutModeEnum.getLayoutMode(mcuNum)));
						SafeSleep.sleepMilliSeconds(300);
					} catch (Exception e) {
						log.error("Send Sip Composite Layout Exception:\n", e);
					}
				}
			} catch (Exception e) {
				log.error("getSipCompositeElements error", e);
			}
		} else {
			log.warn("participants is empty");
		}
	}

	private int getSipCompositeElements(KurentoSession kurentoSession, Participant participant, JsonArray hubPortIds, int mcuNum) {
		HubPort hubPort = null;
		KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
		if (Objects.isNull(hubPort = kurentoParticipant.getPublisher().getSipCompositeHubPort())) {
			hubPort = kurentoParticipant.getPublisher().createSipCompositeHubPort(kurentoSession.getSipComposite());
		}
		kurentoParticipant.getPublisher().getEndpoint().connect(hubPort);
//		kurentoParticipant.getPublisher().internalSinkConnect(kurentoParticipant.getPublisher().getEndpoint(), hubPort);
		hubPortIds.add(hubPort.getId());
		return ++mcuNum;
	}

	private Request<JsonObject> composeLayoutRequestForSip(String pipelineId, String sessionId, JsonArray hubPortIds, LayoutModeEnum layoutMode) {
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
				JsonObject elementsLayout = coordinates.getAsJsonObject().deepCopy();
				elementsLayout.addProperty("connectionId", "connectionId");
				elementsLayout.addProperty("streamType", "streamType");
				elementsLayout.addProperty("object", hubPortIds.get(index.get()).getAsString());
				elementsLayout.addProperty("hasVideo", true);
				elementsLayout.addProperty("onlineStatus", "online");

				index.incrementAndGet();
				layoutInfos.add(elementsLayout);
			}
		});

		JsonObject operationParams = new JsonObject();
		operationParams.add("layoutInfo", layoutInfos);
		params.add("operationParams", operationParams);
		kmsRequest.setMethod("invoke");
		kmsRequest.setParams(params);
		log.info("send sip composite setLayout params:{}", params);

		return kmsRequest;
	}

	private void closePipeline(Runnable callback) {
		synchronized (pipelineReleaseLock) {
			if (pipeline == null) {
				if (callback != null) {
					callback.run();
				}
				return;
			}

			getPipeline().release(new Continuation<Void>() {
				@Override
				public void onSuccess(Void result) throws Exception {
					log.debug("SESSION {}: Released Pipeline", sessionId);
					pipeline = null;
					pipelineLatch = new CountDownLatch(1);
					pipelineCreationErrorCause = null;
					if (callback != null) {
						callback.run();
					}
				}

				@Override
				public void onError(Throwable cause) throws Exception {
					log.warn("SESSION {}: Could not successfully release Pipeline", sessionId, cause);
					pipeline = null;
					pipelineLatch = new CountDownLatch(1);
					pipelineCreationErrorCause = null;
					if (callback != null) {
						callback.run();
					}
				}
			});
		}
	}

	public String getParticipantPrivateIdFromStreamId(String streamId) {
		return this.publishedStreamIds.get(streamId);
	}

	public void restartStatusInKurento(long kmsDisconnectionTime) {

		log.info("Reseting process: reseting remote media objects for active session {}", this.sessionId);

		// Stop recording if session is being recorded
		if (recordingManager.sessionIsBeingRecorded(this.sessionId)) {
			this.recordingManager.forceStopRecording(this, EndReason.mediaServerDisconnect, kmsDisconnectionTime);
		}

		// Close all MediaEndpoints of participants
		this.getParticipants().forEach(p -> {
			KurentoParticipant kParticipant = (KurentoParticipant) p;
			final boolean wasStreaming = kParticipant.isStreaming();
			kParticipant.releaseAllFilters();
			kParticipant.close(EndReason.mediaServerDisconnect, false, kmsDisconnectionTime);
			if (wasStreaming) {
//				kurentoSessionHandler.onUnpublishMedia(kParticipant, this.getParticipants(), null, null, null,
//						EndReason.mediaServerDisconnect);
			}
		});

		// Release pipeline, create a new one and prepare new PublisherEndpoints for
		// allowed users
		log.info("Reseting process: closing media pipeline for active session {}", this.sessionId);
		compositeService.closeMajorShareComposite();
		this.closePipeline(() -> {
			log.info("Reseting process: media pipeline closed for active session {}", this.sessionId);
			createPipeline();
			log.info("Reset pipeline id:{}", this.getPipeline().getId());
			compositeService.setPipeline(this.getPipeline());
			compositeService.createMajorShareComposite();
			try {
				if (!pipelineLatch.await(20, TimeUnit.SECONDS)) {
					throw new Exception("MediaPipleine was not created in 20 seconds");
				}
				getParticipants().forEach(p -> {
//					if (!OpenViduRole.NON_PUBLISH_ROLES.contains(p.getRole())) {
						KurentoParticipant kParticipant = (KurentoParticipant) p;

						kParticipant.resetPublisherEndpoint();
						kParticipant.notifyClient("reconnectSMS", new JsonObject());
//					}
				});
				log.info(
						"Reseting process: media pipeline created and publisher endpoints reseted for active session {}",
						this.sessionId);
			} catch (Exception e) {
				log.error("Error waiting to new MediaPipeline on KurentoSession restart: {}", e.getMessage());
			}
		});
	}

	public void notifyClient(String participarntPrivateId, String method, JsonObject param) {
		kurentoSessionHandler.notifyClient(participarntPrivateId, method, param);
	}


	public Set<Participant> getMajorPartEachExcludeThorConnect() {
		checkClosed();
		return this.participants.values().stream().map(v -> v.get(StreamType.MAJOR.name()))
				.filter(participant -> Objects.nonNull(participant)
						&& !Objects.equals(OpenViduRole.THOR, participant.getRole()))
				.collect(Collectors.toSet());
	}

	public List<DeliveryKmsManager> getDeliveryKmsManagers() {
		return deliveryKmsManagers;
	}

    /**
     * 是否需要新的分发网络
     */
    public boolean needMediaDeliveryKms(int loadFactor) {
		int partSize = this.getMajorPartEachIncludeThorConnect().size();
		int deliveryKmsSize = this.getDeliveryKmsManagers().size();
		return ((partSize - 1) / loadFactor) > deliveryKmsSize;
	}

    public void createDeliveryKms(Kms otherKms, int loadFactor) {
        synchronized (pipelineCreateLock) {
            if (!needMediaDeliveryKms(loadFactor)) {
                return;
            }

			for (DeliveryKmsManager deliveryKmsManager : deliveryKmsManagers) {
				if (Objects.equals(deliveryKmsManager.getKms().getId(), otherKms.getId())) {
					log.info("最低负载的kms已经参与分发，不在重复创建，kmsId {} ,kmsIp {}", otherKms.getId(), otherKms.getIp());
					return;
				}
			}

            DeliveryKmsManager deliveryKms = new DeliveryKmsManager(otherKms, this, kurentoSessionHandler);
            deliveryKmsManagers.add(deliveryKms);
			log.info("create delivery kms sessionId {} kmsId {} ,kmsIp {},deliveryKms {}", this.sessionId, otherKms.getId(), otherKms.getIp(), deliveryKms.getId());
			deliveryKms.initToReady();
        }
    }

	public void notifyPublishChannelPass(KurentoParticipant kurentoParticipant, PublisherEndpoint endpoint) {
		kurentoSessionHandler.notifyPublishMedias(kurentoParticipant, kurentoParticipant.getPublisherStreamId(),
				kurentoParticipant.getPublisher().createdAt(), getSessionId(), endpoint.getMediaOptions(),
				getParticipants(), true, 0, null);
	}
}
