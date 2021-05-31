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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.MediaOptions;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.endpoint.*;
import io.openvidu.server.kurento.kms.EndpointLoadManager;
import io.openvidu.server.living.service.LivingManager;
import io.openvidu.server.recording.service.RecordingManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.kurento.client.*;
import org.kurento.client.internal.server.KurentoServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class KurentoParticipant extends Participant {

	private static final Logger log = LoggerFactory.getLogger(KurentoParticipant.class);

	private OpenviduConfig openviduConfig;
	private RecordingManager recordingManager;
	private LivingManager livingManager;

	private boolean webParticipant = true;

	private final KurentoSession session;
	private KurentoParticipantEndpointConfig endpointConfig;

	private PublisherEndpoint publisher;
	private CountDownLatch publisherLatch = new CountDownLatch(1);

	private final ConcurrentMap<String, Filter> filters = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, SubscriberEndpoint> subscribers = new ConcurrentHashMap<>();

	private String publisherStreamId;

	private final String strMSTagDebugMCUParticipant = "debugMCUParticipant";
	private final String strMSTagDebugEndpointName = "debugEndpointName";
	private final String strMSTagDebugPassThroughName = "debugPassThroughName";


	public KurentoParticipant(Participant participant, KurentoSession kurentoSession,
			KurentoParticipantEndpointConfig endpointConfig, OpenviduConfig openviduConfig,
			RecordingManager recordingManager, LivingManager livingManager) {
		super(participant.getUserId(),participant.getFinalUserId(), participant.getParticipantPrivateId(), participant.getParticipantPublicId(),
				kurentoSession.getSessionId(), participant.getRole(), participant.getStreamType(), participant.getClientMetadata(),
				participant.getLocation(), participant.getPlatform(), participant.getCreatedAt(), participant.getAbility(), participant.getFunctionality());
		setMicStatus(participant.getMicStatus());
		setVideoStatus(participant.getVideoStatus());
		setSharePowerStatus(participant.getSharePowerStatus());
		setHandStatus(participant.getHandStatus());
		setRoomSubject(participant.getRoomSubject());
		setAppShowInfo(participant.getAppShowName(), participant.getAppShowDesc());
		setShareStatus(participant.getShareStatus());
		setSpeakerStatus(participant.getSpeakerStatus());
		setPreset(participant.getPreset());
		setJoinType(participant.getJoinType());
		setParticipantName(participant.getParticipantName());
		setUserType(participant.getUserType());
		setTerminalType(participant.getTerminalType());
		setUuid(participant.getUuid());
		setUserId(participant.getUserId());
		setUsername(participant.getUsername());
		setSubtitleConfig(participant.getSubtitleConfig());
		setSubtitleLanguage(participant.getSubtitleLanguage());
		setApplicationContext(participant.getApplicationContext());
		setOrder(participant.getOrder());
		setProject(participant.getProject());
		setVoiceMode(participant.getVoiceMode());

		this.endpointConfig = endpointConfig;
		this.openviduConfig = openviduConfig;
		this.recordingManager = recordingManager;
		this.livingManager = livingManager;
		this.session = kurentoSession;

		if (!OpenViduRole.NON_PUBLISH_ROLES.contains(participant.getRole())) {
			// Initialize a PublisherEndpoint
			this.publisher = new PublisherEndpoint(webParticipant, this, participant.getParticipantPublicId(),
					this.session.getPipeline(), this.openviduConfig);

			this.publisher.setCompositeService(this.session.compositeService);
		}

		// ↓↓↓↓↓↓↓↓ 杨宇 注释于2021年3月2日17:56:49，
		// 原因：我认为在每次receiveVideoFrom时会调用getNewOrExistingSubscriber并创建一个subscriber，
		// 没有必要在初始化时对每个publisher进行创建并保存，这样造成了内存的浪费
		/*
		for (Participant other : session.getParticipants()) {
			if (!other.getParticipantPublicId().equals(this.getParticipantPublicId())
					&& !OpenViduRole.NON_PUBLISH_ROLES.contains(other.getRole())) {
				// Initialize a SubscriberEndpoint for each other user connected with PUBLISHER
				// or MODERATOR role
				getNewOrExistingSubscriber(other.getParticipantPublicId());
			}
		}
		 */
		// ↑↑↑↑↑↑↑↑↑ 杨宇 注释于2021年3月2日17:56:49，
	}

	public void createPublisher() {
		log.info("#####create publisher when role changed and id:{}", getParticipantName());
		if (!OpenViduRole.NON_PUBLISH_ROLES.contains(getRole()) &&
                (Objects.isNull(publisher) || Objects.isNull(publisher.getCompositeService()))) {
			// Initialize a PublisherEndpoint
			this.publisher = new PublisherEndpoint(webParticipant, this, getParticipantPublicId(),
					this.session.getPipeline(), this.openviduConfig);

			this.publisher.setCompositeService(this.session.compositeService);
		}
	}

	public void createPublishingEndpoint(MediaOptions mediaOptions, Participant participant) {
		if (Objects.isNull(this.publisher)) {
			// Initialize a PublisherEndpoint
			this.publisher = new PublisherEndpoint(webParticipant, this, participant.getParticipantPublicId(),
					this.session.getPipeline(), this.openviduConfig);

			this.publisher.setCompositeService(this.session.compositeService);
		} else if (participant.getRole().needToPublish() && Objects.nonNull(publisher.getMediaOptions())) {
			this.publisher = new PublisherEndpoint(webParticipant, this, participant.getParticipantPublicId(),
					this.session.getPipeline(), this.openviduConfig);

			this.publisher.setCompositeService(this.session.compositeService);
		}
		this.publisher.createEndpoint(publisherLatch);
		if (getPublisher().getEndpoint() == null) {
			this.setStreaming(false);
			throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE, "Unable to create publisher endpoint");
		}
		this.publisher.setMediaOptions(mediaOptions);

		String publisherStreamId;
		if (StringUtils.isEmpty(this.publisherStreamId)) {
			publisherStreamId = this.getParticipantPublicId() + "_"
					+ (mediaOptions.hasVideo() ? mediaOptions.getTypeOfVideo() : "MICRO") + "_"
					+ RandomStringUtils.random(5, true, false).toUpperCase();
			this.publisherStreamId = publisherStreamId;
		} else {
			publisherStreamId = this.publisherStreamId;
		}

		if (Objects.equals(this.session.getConferenceMode(), ConferenceModeEnum.MCU)) {
		    if (Objects.equals(StreamType.SHARING, getStreamType())) {
                this.session.compositeService.setShareStreamId(publisherStreamId);
            }

            if (StringUtils.isEmpty(this.session.compositeService.getMixMajorShareStreamId())) {
                String mixMajorShareStreamId = RandomStringUtils.random(32, true, true)
                        + "_" + "MAJOR-SHARE-MIX";
                this.session.compositeService.setMixMajorShareStreamId(mixMajorShareStreamId);
            }

			this.publisher.getMajorShareHubPort().addTag(strMSTagDebugMCUParticipant, getParticipantName());
		} else if (TerminalTypeEnum.S == getTerminalType()) {
			log.info("sip terminal:{} published {} and create sipComposite", getUuid(), publisherStreamId);
			Composite sipComposite = this.session.createSipComposite();
			this.publisher.createSipCompositeHubPort(sipComposite);
		}

		if (Objects.nonNull(session.getSipComposite())) {
			session.asyncUpdateSipComposite();
		}

		String debugRandomID = RandomStringUtils.randomAlphabetic(6);
		this.publisher.setEndpointName(publisherStreamId);
		this.publisher.getEndpoint().setName(publisherStreamId);
		this.publisher.getEndpoint().addTag(strMSTagDebugEndpointName, getParticipantName() + "_pub_cid_" + debugRandomID);
		this.publisher.getPassThru().addTag(strMSTagDebugPassThroughName, getParticipantName() + "_pt_cid_" + debugRandomID);
		this.publisher.setStreamId(publisherStreamId);
		endpointConfig.addEndpointListeners(this.publisher, "publisher");

		// Remove streamId from publisher's map
		this.session.publishedStreamIds.putIfAbsent(this.getPublisherStreamId(), this.getParticipantPrivateId());
	}

	public synchronized Filter getFilterElement(String id) {
		return filters.get(id);
	}

	public synchronized void removeFilterElement(String id) {
		Filter filter = getFilterElement(id);
		filters.remove(id);
		if (filter != null) {
			publisher.revert(filter);
		}
	}

	public synchronized void releaseAllFilters() {
		// Check this, mutable array?
		filters.forEach((s, filter) -> removeFilterElement(s));
		if (this.publisher != null && this.publisher.getFilter() != null) {
			this.publisher.revert(this.publisher.getFilter());
		}
	}

	public PublisherEndpoint getPublisher() {
		try {
			if (!publisherLatch.await(KurentoSession.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS)) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
						"Timeout reached while waiting for publisher endpoint to be ready");
			}
		} catch (InterruptedException e) {
			throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
					"Interrupted while waiting for publisher endpoint to be ready: " + e.getMessage());
		}
		return this.publisher;
	}

	public boolean isPublisherStreaming() {
		return this.isStreaming() && publisher != null && publisher.getEndpoint() != null;
	}

	public void setPublisher(PublisherEndpoint publisher) {
		this.publisher = publisher;
	}

	public ConcurrentMap<String, SubscriberEndpoint> getSubscribers() {
		return this.subscribers;
	}

	public MediaOptions getPublisherMediaOptions() {
		return this.publisher.getMediaOptions();
	}

	public void setPublisherMediaOptions(MediaOptions mediaOptions) {
		this.publisher.setMediaOptions(mediaOptions);
	}

	public KurentoSession getSession() {
		return session;
	}

	public String publishToRoom(SdpType sdpType, String sdpString, boolean doLoopback,
			MediaElement loopbackAlternativeSrc, MediaType loopbackConnectionType) {
		log.info("PARTICIPANT {}: Request to publish video in room {} (sdp type {})", this.getParticipantPublicId(),
				this.session.getSessionId(), sdpType);
		log.trace("PARTICIPANT {}: Publishing Sdp ({}) is {}", this.getParticipantPublicId(), sdpType, sdpString);

		String sdpResponse = this.getPublisher().publish(sdpType, sdpString, doLoopback, loopbackAlternativeSrc,
				loopbackConnectionType);
		this.streaming = true;

		// deal part default order in the conference
		if (isMcuInclude()) {
			this.session.dealParticipantDefaultOrder(this);
		}

		log.trace("PARTICIPANT {}: Publishing Sdp ({}) is {}", this.getParticipantPublicId(), sdpType, sdpResponse);
		log.info("PARTICIPANT {}: Is now publishing video in room {}", this.getParticipantPublicId(),
				this.session.getSessionId());

		/*if (this.openviduConfig.isRecordingModuleEnabled()
				&& this.recordingManager.sessionIsBeingRecorded(session.getSessionId())) {
			this.recordingManager.startOneIndividualStreamRecording(session, null, null, this);
		}*/

		if (this.openviduConfig.isLivingModuleEnabled()
				&& this.livingManager.sessionIsBeingLived(session.getSessionId())) {
			this.livingManager.startOneIndividualStreamLiving(session, null, null, this);
		}

		endpointConfig.getCdr().recordNewPublisher(this, session.getSessionId(), publisher.getStreamId(),
				publisher.getMediaOptions(), publisher.createdAt());

		return sdpResponse;
	}

	private boolean isMcuInclude() {
		return ConferenceModeEnum.MCU.equals(this.session.getConferenceMode())
				&& !OpenViduRole.NON_PUBLISH_ROLES.contains(this.getRole())
				&& getStreamType().isStreamTypeMixInclude()
				&& !(getStreamType().equals(StreamType.SHARING) && session.isModeratorHasMulticastplay());
	}

	public void unpublishMedia(EndReason reason, long kmsDisconnectionTime) {
		log.info("PARTICIPANT {}: unpublishing media stream from room {}", this.getParticipantPublicId(),
				this.session.getSessionId());
		releasePublisherEndpoint(reason, kmsDisconnectionTime);

		this.publisher = new PublisherEndpoint(webParticipant, this, this.getParticipantPublicId(),
				this.getPipeline(), this.openviduConfig);
		log.info("PARTICIPANT {}: released publisher endpoint and left it initialized (ready for future streaming)",
				this.getParticipantPublicId());
	}

	public String receiveMediaFrom(Participant sender, StreamModeEnum streamMode, String sdpOffer, String externalSenderName) {
		String senderName = sender.getParticipantPublicId();
		if (!StringUtils.isEmpty(externalSenderName)) {
			senderName = externalSenderName;
		}

		log.info("PARTICIPANT {}: Request to receive media from {} in room {}", this.getParticipantPublicId(),
				senderName, this.session.getSessionId());
		log.trace("PARTICIPANT {}: SdpOffer for {} is {}", this.getParticipantPublicId(), senderName, sdpOffer);

		if (!Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) &&
				senderName.equals(this.getParticipantPublicId())) {
			log.warn("PARTICIPANT {}: trying to configure loopback by subscribing", this.getParticipantPublicId());
			throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE, "Can loopback only when publishing media");
		}

		KurentoParticipant kSender = (KurentoParticipant) sender;

		if (kSender.getPublisher() == null) {
			log.warn("PARTICIPANT {}: Trying to connect to a user without " + "a publishing endpoint",
					this.getParticipantPublicId());
			return null;
		}
		PublisherEndpoint senderPublisher = kSender.getPublisher();

		log.debug("PARTICIPANT {}: Creating a subscriber endpoint to user {}", this.getParticipantPublicId(),
				senderName);

		SubscriberEndpoint subscriber = getNewOrExistingSubscriber(senderName);
		if (subscriber.getEndpoint() == null) {
			subscriber = getNewOrExistingSubscriber(senderName);
			if (!getRole().needToPublish() && !getSession().getDeliveryKmsManagers().isEmpty() && getRole() != OpenViduRole.THOR) {
				DeliveryKmsManager deliveryKms = EndpointLoadManager.getLessDeliveryKms(getSession().getDeliveryKmsManagers());
				MediaChannel mediaChannel = senderPublisher.getMediaChannels().get(deliveryKms.getId());
				if (mediaChannel == null) {
					log.warn("mediaChannel not exist");
					synchronized (this) {
						mediaChannel = deliveryKms.dispatcher(kSender);
					}
				}
				log.info("mediaChannel state = {}", mediaChannel.getState().name());
				if (mediaChannel.waitToReady() && mediaChannel.getState().isAvailable()) {
					log.debug("PARTICIPANT {}: Creating a subscriber endpoint to user {}", this.getParticipantPublicId(),
							senderName);
					log.info("uuid({}) 订阅 分发的uuid({}) targetPipeline {}  mediaChannel.publisher={}",
							this.getUuid(), kSender.getUuid(), mediaChannel.getTargetPipeline(), mediaChannel.getPublisher().getEndpoint().getId());
					senderPublisher = mediaChannel.getPublisher();
					subscriber = getNewAndCompareSubscriber(senderName, mediaChannel.getTargetPipeline(), subscriber);
				} else {
					log.info("media channel not ready:{},use master kms", mediaChannel.getState().name());
				}
			}
		}

		try {
			CountDownLatch subscriberLatch = new CountDownLatch(1);
			SdpEndpoint oldMediaEndpoint = subscriber.createEndpoint(subscriberLatch);
			try {
				if (!subscriberLatch.await(KurentoSession.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS)) {
					throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
							"Timeout reached when creating subscriber endpoint");
				}
			} catch (InterruptedException e) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
						"Interrupted when creating subscriber endpoint: " + e.getMessage());
			}
			if (oldMediaEndpoint != null) {
				log.warn(
						"PARTICIPANT {}: Two threads are trying to create at "
								+ "the same time a subscriber endpoint for user {}",
						this.getParticipantPublicId(), senderName);
				return null;
			}
			if (subscriber.getEndpoint() == null) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE, "Unable to create subscriber endpoint");
			}

			String subscriberEndpointName = this.getParticipantPublicId() + "_" + kSender.getPublisherStreamId();

			subscriber.setEndpointName(subscriberEndpointName);
			subscriber.getEndpoint().setName(subscriberEndpointName);
			subscriber.getEndpoint().addTag(strMSTagDebugEndpointName, getParticipantName() + "_sub_" + kSender.getUuid() +
					"_" + kSender.getStreamType() + "_cid_" + RandomStringUtils.randomAlphabetic(6));
			subscriber.setStreamId(kSender.getPublisherStreamId());

			endpointConfig.addEndpointListeners(subscriber, "subscriber");

		} catch (OpenViduException e) {
			this.subscribers.remove(senderName);
			throw e;
		}

		log.debug("PARTICIPANT {}: Created subscriber endpoint for user {}", this.getParticipantPublicId(), senderName);
		try {
			String sdpAnswer = subscriber.subscribeVideo(sdpOffer, senderPublisher, streamMode);
			log.trace("PARTICIPANT {}: Subscribing SdpAnswer is {}", this.getParticipantPublicId(), sdpAnswer);
			log.info("PARTICIPANT {}: Is now receiving video from {} in room {}", this.getParticipantPublicId(),
					senderName, this.session.getSessionId());

			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(this.getParticipantPublicId())) {
				endpointConfig.getCdr().recordNewSubscriber(this, this.session.getSessionId(),
						sender.getPublisherStreamId(), sender.getParticipantPublicId(), subscriber.createdAt());
			}

			if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU) &&
                    Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode)) {
				if (!OpenViduRole.NON_PUBLISH_ROLES.contains(getRole())) {
					subscriber.subscribeAudio(this.getPublisher());
				} else {
					subscriber.subscribeAudio(null);
				}
			}

			return sdpAnswer;
		} catch (KurentoServerException e) {
			// TODO Check object status when KurentoClient sets this info in the object
			if (e.getCode() == 40101) {
				log.warn("Publisher endpoint was already released when trying "
						+ "to connect a subscriber endpoint to it", e);
			} else {
				log.error("Exception connecting subscriber endpoint " + "to publisher endpoint", e);
			}
			this.subscribers.remove(senderName);
			releaseSubscriberEndpoint(senderName, subscriber, null);
		}
		return null;
	}

	public String receiveMediaFromDelivery(Participant sender, StreamModeEnum streamMode, String sdpOffer, String externalSenderName, DeliveryKmsManager deliveryKms) {
		log.info("{} into receiveMediaFromDelivery", this.getUuid());

		String senderName = sender.getParticipantPublicId();
		if (!StringUtils.isEmpty(externalSenderName)) {
			senderName = externalSenderName;
		}

		log.info("PARTICIPANT {}: Request to receive delivery media from {} in room {}", this.getParticipantPublicId(),
				senderName, this.session.getSessionId());
		log.trace("PARTICIPANT {}: SdpOffer for {} is {}", this.getParticipantPublicId(), senderName, sdpOffer);

		if (!Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode) &&
				senderName.equals(this.getParticipantPublicId())) {
			log.warn("PARTICIPANT {}: trying to configure loopback by subscribing", this.getParticipantPublicId());
			throw new OpenViduException(Code.USER_NOT_STREAMING_ERROR_CODE, "Can loopback only when publishing media");
		}

		KurentoParticipant kSender = (KurentoParticipant) sender;

		if (kSender.getPublisher() == null) {
			log.warn("PARTICIPANT {}: Trying to connect to a user without " + "a publishing endpoint",
					this.getParticipantPublicId());
			return null;
		}
// 这里开始不同
		MediaChannel mediaChannel = kSender.getPublisher().getMediaChannels().get(deliveryKms.getId());
		if (mediaChannel == null) {
			log.warn("mediaChannel not exist");
			synchronized (this) {
				mediaChannel = deliveryKms.dispatcher(kSender);
//				mediaChannel = new MediaChannel(session.getPipeline(), kSender.getPublisher().getPassThru(), deliveryKms.getPipeline(),
//						true, kSender, kSender.getPublisherStreamId(), openviduConfig);
				kSender.getPublisher().getMediaChannels().put(deliveryKms.getId(), mediaChannel);
				//mediaChannel.createChannel();
			}
		}
		log.info("mediaChannel state = {}", mediaChannel.getState().name());
		//mediaChannel = new ArrayList<>(kSender.getMediaChannels().values()).get(0);

		log.debug("PARTICIPANT {}: Creating a subscriber endpoint to user {}", this.getParticipantPublicId(),
				senderName);
		log.info("uuid({}) 订阅 分发的uuid({}) targetPipeline {}  mediaChannel.publisher={}",
				this.getUuid(), kSender.getUuid(), mediaChannel.getTargetPipeline(), mediaChannel.getPublisher().getEndpoint().getId());
		// 这里开始不同
		SubscriberEndpoint subscriber = getNewAndCompareSubscriber(senderName, mediaChannel.getTargetPipeline(), null);


		try {
			CountDownLatch subscriberLatch = new CountDownLatch(1);
			SdpEndpoint oldMediaEndpoint = subscriber.createEndpoint(subscriberLatch);
			try {
				if (!subscriberLatch.await(KurentoSession.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS)) {
					throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
							"Timeout reached when creating subscriber endpoint");
				}
			} catch (InterruptedException e) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
						"Interrupted when creating subscriber endpoint: " + e.getMessage());
			}
			if (oldMediaEndpoint != null) {
				log.warn(
						"PARTICIPANT {}: Two threads are trying to create at "
								+ "the same time a subscriber endpoint for user {}",
						this.getParticipantPublicId(), senderName);
				return null;
			}
			if (subscriber.getEndpoint() == null) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE, "Unable to create subscriber endpoint");
			}

			String subscriberEndpointName = this.getParticipantPublicId() + "_" + kSender.getPublisherStreamId();

			subscriber.setEndpointName(subscriberEndpointName);
			subscriber.getEndpoint().setName(subscriberEndpointName);
			subscriber.getEndpoint().addTag(strMSTagDebugEndpointName, getParticipantName() + "_sub_" + kSender.getUuid() +
					"_" + kSender.getStreamType() + "_cid_" + RandomStringUtils.randomAlphabetic(6));
			subscriber.setStreamId(kSender.getPublisherStreamId());

			endpointConfig.addEndpointListeners(subscriber, "subscriber");

		} catch (OpenViduException e) {
			this.subscribers.remove(senderName);
			throw e;
		}

		log.debug("PARTICIPANT {}: Created subscriber endpoint for user {}", this.getParticipantPublicId(), senderName);
		try {
			String sdpAnswer = subscriber.subscribeVideo(sdpOffer, mediaChannel.getPublisher() , streamMode);
			subscriber.internalAddIceCandidateCache();
			log.trace("PARTICIPANT {}: Subscribing SdpAnswer is {}", this.getParticipantPublicId(), sdpAnswer);
			log.info("PARTICIPANT {}: Is now receiving video from {} in room {}", this.getParticipantPublicId(),
					senderName, this.session.getSessionId());

			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(this.getParticipantPublicId())) {
				endpointConfig.getCdr().recordNewSubscriber(this, this.session.getSessionId(),
						sender.getPublisherStreamId(), sender.getParticipantPublicId(), subscriber.createdAt());
			}

			if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU) &&
					Objects.equals(StreamModeEnum.MIX_MAJOR_AND_SHARING, streamMode)) {
				if (!OpenViduRole.NON_PUBLISH_ROLES.contains(getRole())) {
					subscriber.subscribeAudio(this.getPublisher());
				} else {
					subscriber.subscribeAudio(null);
				}
			}

			return sdpAnswer;
		} catch (KurentoServerException e) {
			// TODO Check object status when KurentoClient sets this info in the object
			if (e.getCode() == 40101) {
				log.warn("Publisher endpoint was already released when trying "
						+ "to connect a subscriber endpoint to it", e);
			} else {
				log.error("Exception connecting subscriber endpoint " + "to publisher endpoint", e);
			}
			this.subscribers.remove(senderName);
			releaseSubscriberEndpoint(senderName, subscriber, null);
		}
		return null;
	}

	public void cancelReceivingMedia(String senderName, EndReason reason) {
		log.info("PARTICIPANT {}: cancel receiving media from {}", this.getParticipantPublicId(), senderName);
		SubscriberEndpoint subscriberEndpoint = subscribers.remove(senderName);
		if (subscriberEndpoint == null || subscriberEndpoint.getEndpoint() == null) {
			log.warn("PARTICIPANT {}: Trying to cancel receiving video from user {}. "
					+ "But there is no such subscriber endpoint.", this.getParticipantPublicId(), senderName);
		} else {
			releaseSubscriberEndpoint(senderName, subscriberEndpoint, reason);
			log.info("PARTICIPANT {}: stopped receiving media from {} in room {}", this.getParticipantPublicId(),
					senderName, this.session.getSessionId());
		}
	}

	public void close(EndReason reason, boolean definitelyClosed, long kmsDisconnectionTime) {
		log.debug("PARTICIPANT {}: Closing user", this.getParticipantPublicId());
		if (isClosed()) {
			log.warn("PARTICIPANT {}: Already closed", this.getParticipantPublicId());
			return;
		}
		this.closed = definitelyClosed;
		for (String remoteParticipantName : subscribers.keySet()) {
			SubscriberEndpoint subscriber = this.subscribers.get(remoteParticipantName);
			if (subscriber != null && subscriber.getEndpoint() != null) {
				releaseSubscriberEndpoint(remoteParticipantName, subscriber, reason);
				log.debug("PARTICIPANT {}: Released subscriber endpoint to {}", this.getParticipantPublicId(),
						remoteParticipantName);
			} else {
				log.warn(
						"PARTICIPANT {}: Trying to close subscriber endpoint to {}. "
								+ "But the endpoint was never instantiated.",
						this.getParticipantPublicId(), remoteParticipantName);
			}
		}
		this.subscribers.clear();
		releasePublisherEndpoint(reason, kmsDisconnectionTime);
		if (Objects.equals(StreamType.SHARING, getStreamType()) &&
				!Objects.isNull(session.compositeService.getShareStreamId())) {
			session.compositeService.setShareStreamId(null);
		}
	}

	/**
	 * Returns a {@link SubscriberEndpoint} for the given participant public id. The
	 * endpoint is created if not found.
	 *
	 * @param senderPublicId id of another user
	 * @return the endpoint instance
	 */
	public SubscriberEndpoint getNewOrExistingSubscriber(String senderPublicId) {
		SubscriberEndpoint subscriberEndpoint = new SubscriberEndpoint(webParticipant, this, senderPublicId,
				this.getPipeline(), this.session.compositeService, this.openviduConfig);

		SubscriberEndpoint existingSendingEndpoint = this.subscribers.putIfAbsent(senderPublicId, subscriberEndpoint);
		if (existingSendingEndpoint != null) {
			subscriberEndpoint = existingSendingEndpoint;
			log.trace("PARTICIPANT {}: Already exists a subscriber endpoint to user {}", this.getParticipantPublicId(),
					senderPublicId);
		} else {
			log.debug("PARTICIPANT {}: New subscriber endpoint to user {}", this.getParticipantPublicId(), senderPublicId);
		}

		return subscriberEndpoint;
	}

	public SubscriberEndpoint getNewAndCompareSubscriber(String senderPublicId, MediaPipeline pipeline, SubscriberEndpoint compare) {
		SubscriberEndpoint subscriberEndpoint = new SubscriberEndpoint(webParticipant, this, senderPublicId,
				pipeline, this.session.compositeService, this.openviduConfig);
		SubscriberEndpoint existingSendingEndpoint = this.subscribers.putIfAbsent(senderPublicId, subscriberEndpoint);
		if (existingSendingEndpoint != null) {
			if (existingSendingEndpoint == compare) {
				this.subscribers.put(senderPublicId, subscriberEndpoint);
			} else {
				subscriberEndpoint = existingSendingEndpoint;
			}
			log.trace("PARTICIPANT {}: Already exists a subscriber endpoint to user {}", this.getParticipantPublicId(),
					senderPublicId);
		} else {
			log.debug("PARTICIPANT {}: New subscriber endpoint to user {},pipeline {}", this.getParticipantPublicId(), senderPublicId, subscriberEndpoint.getPipeline());
		}

		return subscriberEndpoint;
	}

	public void addIceCandidate(String endpointName, IceCandidate iceCandidate) {
		if (this.getParticipantPublicId().equals(endpointName)) {
			if (Objects.isNull(this.publisher)) {
				// Initialize a PublisherEndpoint
				this.publisher = new PublisherEndpoint(webParticipant, this, getParticipantPublicId(),
						this.session.getPipeline(), this.openviduConfig);

				this.publisher.setCompositeService(this.session.compositeService);
			}
			this.publisher.addIceCandidate(iceCandidate);
		} else {
			this.getNewOrExistingSubscriber(endpointName).addIceCandidate(iceCandidate);
		}
	}

	public void sendIceCandidate(String senderPublicId, String endpointName, IceCandidate candidate) {
		session.sendIceCandidate(this.getParticipantPrivateId(), senderPublicId, endpointName, candidate);
	}

	public void sendMediaError(ErrorEvent event) {
		String desc = event.getType() + ": " + event.getDescription() + "(errCode=" + event.getErrorCode() + ")";
		log.warn("PARTICIPANT {}: Media error encountered: {}", getParticipantPublicId(), desc);
		session.sendMediaError(this.getParticipantPrivateId(), desc);
	}

	private void releasePublisherEndpoint(EndReason reason, long kmsDisconnectionTime) {
		if (publisher != null && publisher.getEndpoint() != null) {
			// 释放分发资源
			if (!publisher.getMediaChannels().isEmpty()) {
				log.info("release mediaChannels reason {}", reason);
				publisher.getMediaChannels().values().forEach(MediaChannel::release);
				publisher.getMediaChannels().clear();
			}

			// Remove streamId from publisher's map
			if (!StringUtils.isEmpty(this.getPublisherStreamId())) {
				this.session.publishedStreamIds.remove(this.getPublisherStreamId());
			}

			if (this.openviduConfig.isLivingModuleEnabled()
					&& this.livingManager.sessionIsBeingLived(session.getSessionId())) {
				this.livingManager.stopOneIndividualStreamLiving(session, this.getPublisherStreamId(),
						kmsDisconnectionTime);
			}

			publisher.unregisterErrorListeners();
			if (publisher.kmsWebrtcStatsThread != null) {
				publisher.kmsWebrtcStatsThread.cancel(true);
			}

			for (MediaElement el : publisher.getMediaElements()) {
				releaseElement(getParticipantPublicId(), el);
				log.info("Release publisher self mediaElement and object id:{}", el.getId());
			}
			if (Objects.nonNull(publisher)) {
				publisher.closeAudioComposite();
				releaseElement(getParticipantPublicId(), publisher.getEndpoint());
//				endpointConfig.getCdr().stopPublisher(this.getParticipantPublicId(), publisher.getStreamId(), reason);
			}
			if (Objects.nonNull(publisher.getSipCompositeHubPort())) {
				releaseElement(getParticipantPublicId(), publisher.getSipCompositeHubPort());
				session.asyncUpdateSipComposite();
			}
			this.session.deregisterPublisher();

			publisher = null;
			setStreaming(false);
		} else {
			log.warn("PARTICIPANT {}: Trying to release publisher endpoint but is null", getParticipantPublicId());
		}



	}

	private void releaseSubscriberEndpoint(String senderName, SubscriberEndpoint subscriber, EndReason reason) {
		if (subscriber != null) {

			subscriber.unregisterErrorListeners();
			if (subscriber.kmsWebrtcStatsThread != null) {
				subscriber.kmsWebrtcStatsThread.cancel(true);
			}

			releaseElement(senderName, subscriber.getEndpoint());

			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(this.getParticipantPublicId())) {
				endpointConfig.getCdr().stopSubscriber(this.getParticipantPublicId(), senderName,
						subscriber.getStreamId(), reason);
			}

		} else {
			log.warn("PARTICIPANT {}: Trying to release subscriber endpoint for '{}' but is null",
					this.getParticipantPublicId(), senderName);
		}
	}

	public void releaseElement(final String senderName, final MediaElement element) {
		if (Objects.isNull(element)) return;
		final String eid = element.getId();
		try {
			element.release(new Continuation<Void>() {
				@Override
				public void onSuccess(Void result) throws Exception {
					log.debug("PARTICIPANT {}: Released successfully media element #{} for {}",
							getParticipantPublicId(), eid, senderName);
				}

				@Override
				public void onError(Throwable cause) throws Exception {
					log.warn("PARTICIPANT {}: Could not release media element #{} for {}", getParticipantPublicId(),
							eid, senderName, cause);
				}
			});
		} catch (Exception e) {
			log.error("PARTICIPANT {}: Error calling release on elem #{} for {}", getParticipantPublicId(), eid,
					senderName, e);
		}
	}

	public MediaPipeline getPipeline() {
		return this.session.getPipeline();
	}

	@Override
	public String getPublisherStreamId() {
		return this.publisher.getStreamId();
	}

	public void resetPublisherEndpoint() {
		log.info("Reseting publisher endpoint for participant {}", this.getParticipantPublicId());
        if (!OpenViduRole.NON_PUBLISH_ROLES.contains(this.getRole())) {
            this.publisher = new PublisherEndpoint(webParticipant, this, this.getParticipantPublicId(),
                    this.session.getPipeline(), this.openviduConfig);

            this.publisher.setCompositeService(this.session.compositeService);
        }
	}

	public void notifyClient(String method, JsonObject param) {
		this.session.notifyClient(this.participantPrivatetId, method, param);
	}

	@Override
	public JsonObject toJson() {
		return this.sharedJson(MediaEndpoint::toJson);
	}

	public JsonObject withStatsToJson() {
		return this.sharedJson(MediaEndpoint::withStatsToJson);
	}

	private JsonObject sharedJson(Function<MediaEndpoint, JsonObject> toJsonFunction) {
		JsonObject json = super.toJson();
		JsonArray publisherEnpoints = new JsonArray();
		JsonArray mediaChannels = new JsonArray();
		if (this.streaming && this.publisher.getEndpoint() != null) {
			publisherEnpoints.add(toJsonFunction.apply(this.publisher));
			if (!this.publisher.getMediaChannels().isEmpty()){
				for (MediaChannel mediaChannel : this.publisher.getMediaChannels().values()) {
					mediaChannels.add(mediaChannel.toJson());
				}
			}
		}
		JsonArray subscriberEndpoints = new JsonArray();
		for (MediaEndpoint sub : this.subscribers.values()) {
			if (sub.getEndpoint() != null) {
				subscriberEndpoints.add(toJsonFunction.apply(sub));
			}
		}
		json.add("publishers", publisherEnpoints);
		json.add("mediaChannels", mediaChannels);
		json.add("subscribers", subscriberEndpoints);
		return json;
	}

	public boolean isMixIncluded() {
		JsonArray mixArr = session.getCurrentPartInMcuLayout();
		for (JsonElement jsonElement : mixArr) {
			JsonObject jsonObject;
			if ((jsonObject = jsonElement.getAsJsonObject()).has("connectionId") &&
					getParticipantPublicId().equals(jsonObject.get("connectionId").getAsString())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Exclude MCU
	 * @param operation on,off
	 * @param publicIds participants' publicId that this participant receive video from
	 */
	void switchVoiceModeInSession(VoiceMode operation, Set<String> publicIds) {
		Set<Participant> participants = getSession().getMajorAndMinorPartEachConnect();
		if (!CollectionUtils.isEmpty(participants)) {
			participants.forEach(participant -> {
				String subToPartPublicId;
				if (!Objects.equals(subToPartPublicId = participant.getParticipantPublicId(), this.getParticipantPublicId())
						&& publicIds.contains(subToPartPublicId) && participant.isStreaming()) {
					log.info("PARTICIPANT {}: Is now {} receiving video from {} in room {}",
							this.getParticipantPublicId(), Objects.equals(operation, VoiceMode.on) ?
									"stop" : "start", subToPartPublicId, this.session.getSessionId());
					KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
					SubscriberEndpoint subscriberEndpoint = subscribers.get(subToPartPublicId);
					if (Objects.nonNull(subscriberEndpoint)) {
						subscriberEndpoint.controlMediaTypeLink(kurentoParticipant.getPublisher(), MediaType.VIDEO, operation);
					}
				}
			});
		}
	}

	/**
	 * pause or resume stream
	 * @param targetPart Participant
	 * @param operation on,off
	 * @param mediaType video,audio
	 * @param publicIds participants' publicId that this participant receive video or audio from
	 */
	void pauseAndResumeStreamInSession(Participant targetPart, OperationMode operation, String mediaType, Set<String> publicIds) {
		if (publicIds.contains(targetPart.getParticipantPublicId())) {
			log.info("PARTICIPANT {}: Is now {} receiving {} from {} in room {}",
					this.getParticipantPublicId(), Objects.equals(operation, OperationMode.on) ?
							"resume" : "pause", mediaType,this.getParticipantPublicId(), this.session.getSessionId());
			KurentoParticipant kurentoParticipant = (KurentoParticipant) targetPart;
			SubscriberEndpoint subscriberEndpoint = subscribers.get(targetPart.getParticipantPublicId());
			if (Objects.nonNull(subscriberEndpoint)) {
				subscriberEndpoint.controlMediaTypeLink(kurentoParticipant.getPublisher(), MediaType.valueOf(mediaType.toUpperCase()), Objects.equals(operation,OperationMode.on) ? VoiceMode.off : VoiceMode.on);
			}
		} else {
			log.info("PARTICIPANT {}: Is not subscriber {} from {} in room {}",
					this.getParticipantPublicId(), mediaType,this.getParticipantPublicId(), this.session.getSessionId());
		}
	}

	public void notifyPublishChannelPass(PublisherEndpoint endpoint) {
		session.notifyPublishChannelPass(this, endpoint);
	}

}
