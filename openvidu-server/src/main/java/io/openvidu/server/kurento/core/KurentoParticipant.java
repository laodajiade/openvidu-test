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
import io.openvidu.server.exception.BizException;
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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

	//todo 2.0 Deprecated,使用publishers代替
	@Deprecated
	private PublisherEndpoint publisher;

	private final Map<StreamType, PublisherEndpoint> publishers = new ConcurrentHashMap<>();
	//private CountDownLatch publisherLatch = new CountDownLatch(1);

	private final ConcurrentMap<String, Filter> filters = new ConcurrentHashMap<>();
    /**
     * key = subscribeId , value = ep
     */
    private final ConcurrentMap<String, SubscriberEndpoint> subscribers = new ConcurrentHashMap<>();


	private final Object createPublisherLock = new Object();

	private final String strMSTagDebugEndpointName = "debugEndpointName";
	private final String strMSTagDebugPassThroughName = "debugPassThroughName";


	public KurentoParticipant(Participant participant, KurentoSession kurentoSession,
			KurentoParticipantEndpointConfig endpointConfig, OpenviduConfig openviduConfig,
			RecordingManager recordingManager, LivingManager livingManager) {
		super(participant.getUserId(), participant.getParticipantPrivateId(), participant.getParticipantPublicId(),
				kurentoSession.getSessionId(), participant.getRole(), participant.getClientMetadata(),
				participant.getLocation(), participant.getPlatform(), participant.getDeviceModel(), participant.getCreatedAt(), participant.getAbility(), participant.getFunctionality());
		setMicStatus(participant.getMicStatus());
		setVideoStatus(participant.getVideoStatus());
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



		// ↓↓↓↓↓↓↓↓ 杨宇 注释于2021年3月2日17:56:49，
		// 原因：我认为在每次receiveVideoFrom时会调用getNewOrExistingSubscriber并创建一个subscriber，
		// 没有必要在初始化时对每个publisher进行创建并保存，这样造成了内存的浪费
		/*
		if (!OpenViduRole.NON_PUBLISH_ROLES.contains(participant.getRole())) {
			// Initialize a PublisherEndpoint
			this.publisher = new PublisherEndpoint(webParticipant, this, participant.getParticipantPublicId(),
					this.session.getPipeline(), this.openviduConfig);

			this.publisher.setCompositeService(this.session.compositeService);
		}
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

	// delete 2.0
//	public void createPublisher() {
//		log.info("#####create publisher when role changed and id:{}", getParticipantName());
//		if (!OpenViduRole.NON_PUBLISH_ROLES.contains(getRole()) &&
//                (Objects.isNull(publisher) || Objects.isNull(publisher.getCompositeService()))) {
//			// Initialize a PublisherEndpoint
//			this.publisher = new PublisherEndpoint(webParticipant, this, getParticipantPublicId(),
//					this.session.getPipeline(), StreamType.MAJOR,this.openviduConfig);
//
//			this.publisher.setCompositeService(this.session.getCompositeService());
//		}
//	}

	public PublisherEndpoint createPublishingEndpoint(MediaOptions mediaOptions, StreamType streamType) {

		PublisherEndpoint publisher;
		publisher = createPublisher(streamType);

		publisher.createEndpoint(publisher.getPublisherLatch());
        if (getPublisher(streamType).getEndpoint() == null) {
            this.setStreaming(false);
            throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE, "Unable to create publisher endpoint");
        }
        publisher.setMediaOptions(mediaOptions);

        if (TerminalTypeEnum.S == getTerminalType() && this.session.getConferenceMode() != ConferenceModeEnum.MCU) {
            log.info("sip terminal:{} published {} and create sipComposite", getUuid(), publisher.getEndpointName());
            session.getCompositeService().createComposite();
        }

        if (Objects.equals(ConferenceModeEnum.MCU, session.getConferenceMode())) {
            // session.asyncUpdateSipComposite();
            log.info("session.compositeService.updateComposite()  ");
            session.getCompositeService().asyncUpdateComposite();
        }

		String debugRandomID = RandomStringUtils.randomAlphabetic(6);
		publisher.getEndpoint().setName(publisher.getEndpointName());
		publisher.getEndpoint().addTag(strMSTagDebugEndpointName, getParticipantName() + "_pub_cid_" + debugRandomID);
		publisher.getPassThru().addTag(strMSTagDebugPassThroughName, getParticipantName() + "_pt_cid_" + debugRandomID);
		endpointConfig.addEndpointListeners(publisher, "publisher_" + streamType);

		return publisher;
	}

	public PublisherEndpoint createPublisher(StreamType streamType) {
		PublisherEndpoint publisher;
		synchronized (createPublisherLock) {
			publisher = publishers.get(streamType);
			if (Objects.isNull(publisher)) {
				// Initialize a PublisherEndpoint
				publisher = new PublisherEndpoint(webParticipant, this, this.getUuid(),
						this.session.getPipeline(), streamType, this.openviduConfig);

				publisher.setCompositeService(this.session.getCompositeService());
			} else if (this.getRole().needToPublish() && Objects.nonNull(publisher.getMediaOptions())) {
				log.info("fffffffffffffffffffffffffffffff {} {}",this.getUuid(),streamType);
				//todo 2.0 这里好像有个old publisher 泄露了
//				publisher = new PublisherEndpoint(webParticipant, this, this.getParticipantPublicId(),
//						this.session.getPipeline(), streamType, this.openviduConfig);
//				publisher.setCompositeService(this.session.getCompositeService());
			}
			publishers.put(streamType, publisher);
		}
		return publisher;
	}

	public synchronized Filter getFilterElement(String id) {
		return filters.get(id);
	}

	public synchronized void removeFilterElement(String id) {
		Filter filter = getFilterElement(id);
		filters.remove(id);
		if (filter != null) {
			//publisher.revert(filter);
		}
	}

	public synchronized void releaseAllFilters() {
		// Check this, mutable array?
		filters.forEach((s, filter) -> removeFilterElement(s));
//		if (this.publisher != null && this.publisher.getFilter() != null) {
//			this.publisher.revert(this.publisher.getFilter());
//		}
	}

	@Override
	public PublisherEndpoint getPublisher(StreamType streamType) {
		PublisherEndpoint publisherEndpoint = this.publishers.get(streamType);
		if (publisherEndpoint == null) {
			log.warn(" getPublisher publisherEndpoint is null {} {}", this.getUuid(), streamType);
			return null;
		}
		try {
			if (!publisherEndpoint.getPublisherLatch().await(KurentoSession.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS)) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
						"Timeout reached while waiting for publisher endpoint to be ready");
			}
		} catch (InterruptedException e) {
			throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
					"Interrupted while waiting for publisher endpoint to be ready: " + e.getMessage());
		}
		return publisherEndpoint;
	}

	public PublisherEndpoint getPublisher(String publishId) {
		Optional<PublisherEndpoint> any = this.getPublishers().values().stream().filter(ep -> ep.getStreamId().equals(publishId)).findAny();
		if (!any.isPresent()) {
			log.error("getPublisher by streamId publisherEndpoint is null {} {}", this.getUuid(), publishId);
			throw new BizException(ErrorCodeEnum.SERVER_INTERNAL_ERROR);
		}

		try {
			if (!any.get().getPublisherLatch().await(KurentoSession.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS)) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
						"Timeout reached while waiting for publisher endpoint to be ready");
			}
		} catch (InterruptedException e) {
			throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
					"Interrupted while waiting for publisher endpoint to be ready: " + e.getMessage());
		}
		return any.get();
	}

	//todo 2.0 Deprecated
	@Deprecated
	public PublisherEndpoint getPublisher() {
//		try {
//			if (!publisherLatch.await(KurentoSession.ASYNC_LATCH_TIMEOUT, TimeUnit.SECONDS)) {
//				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
//						"Timeout reached while waiting for publisher endpoint to be ready");
//			}
//		} catch (InterruptedException e) {
//			throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
//					"Interrupted while waiting for publisher endpoint to be ready: " + e.getMessage());
//		}
//		return this.publisher;
		return getPublisher(StreamType.MAJOR);
	}

	public Map<StreamType, PublisherEndpoint> getPublishers() {
		return this.publishers;
	}

	//todo 2.0 需要修改
	public boolean isPublisherStreaming() {
		return this.isStreaming() && publisher != null && publisher.getEndpoint() != null;
	}

	public void setPublisher(StreamType streamType, PublisherEndpoint publisher) {
		this.publishers.put(streamType, publisher);
	}

	@Override
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
			MediaElement loopbackAlternativeSrc, MediaType loopbackConnectionType,StreamType streamType) {
		log.info("PARTICIPANT {}: Request to publish video in room {} (sdp type {})", this.getParticipantPublicId(),
				this.session.getSessionId(), sdpType);
		log.trace("PARTICIPANT {}: Publishing Sdp ({}) is {}", this.getParticipantPublicId(), sdpType, sdpString);

		String sdpResponse = this.getPublisher(streamType).publish(sdpType, sdpString, doLoopback, loopbackAlternativeSrc,
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

		endpointConfig.getCdr().recordNewPublisher(this, session.getSessionId(), this.getPublisher(streamType).getStreamId(),
				this.getPublisher(streamType).getMediaOptions(), this.getPublisher(streamType).createdAt());

		return sdpResponse;
	}

	private boolean isMcuInclude() {
		return ConferenceModeEnum.MCU.equals(this.session.getConferenceMode())
				&& !OpenViduRole.NON_PUBLISH_ROLES.contains(this.getRole())
				&& !session.isShare(this.getUuid()) && session.isModeratorHasMulticastplay();
	}

	public void unpublishMedia(PublisherEndpoint publisherEndpoint, EndReason reason, long kmsDisconnectionTime) {
		log.info("PARTICIPANT {}: unpublishing media stream from room {}", this.getParticipantPublicId(),
				this.session.getSessionId());
		releasePublisherEndpoint(publisherEndpoint, reason, kmsDisconnectionTime);

		log.info("PARTICIPANT {}: released publisher endpoint and left it initialized (ready for future streaming)",
				this.getParticipantPublicId());
	}

	/**
	 * @param returnObj 用来接收其他返回参数到上层方法
	 * @return
	 */
	public String receiveMediaFrom(KurentoParticipant sender, StreamModeEnum streamMode, String sdpOffer, StreamType streamType,
								   String publishStreamId, Map<String, Object> returnObj) {
		String subscriberStreamId = null;
		PublisherEndpoint senderPublisher = null;
		if (!Objects.equals(StreamModeEnum.MIX_MAJOR, streamMode)) {
			senderPublisher = sender.getPublisher(streamType);
			if (senderPublisher == null) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE, "没有找到 senderPublisher");
			}
			if (!senderPublisher.getStreamId().equals(publishStreamId)) {
				log.warn("{} 拉的{},{}的流的streamId不匹配{}!={},自动修正", this.getUuid(), sender.getUuid(),
						streamType.name(), senderPublisher.getStreamId(), publishStreamId);
				publishStreamId = senderPublisher.getStreamId();
			}
			log.info("PARTICIPANT {}: Request to receive media from {} in room {}", this.getUuid(),
					publishStreamId, this.session.getSessionId());
			subscriberStreamId = translateSubscribeId(this.getUuid(), publishStreamId);
			log.info("PARTICIPANT {}: Creating a subscriber endpoint to user {}", this.getUuid(),
					subscriberStreamId);
		} else {
			subscriberStreamId = translateSubscribeId(this.getUuid(), publishStreamId);
		}


		SubscriberEndpoint subscriber = getNewOrExistingSubscriber(subscriberStreamId);
		if (subscriber.getEndpoint() == null && !Objects.equals(StreamModeEnum.MIX_MAJOR, streamMode)) {
			subscriber = getNewOrExistingSubscriber(subscriberStreamId);
			if (!getRole().needToPublish() && !getSession().getDeliveryKmsManagers().isEmpty() && getRole() != OpenViduRole.THOR) {
				DeliveryKmsManager deliveryKms = EndpointLoadManager.getLessDeliveryKms(getSession().getDeliveryKmsManagers());
				MediaChannel mediaChannel = senderPublisher.getMediaChannels().get(deliveryKms.getId());
				if (mediaChannel == null) {
					log.warn("mediaChannel not exist");
					synchronized (this) {
						mediaChannel = deliveryKms.dispatcher(sender, senderPublisher);
					}
				}
				log.info("mediaChannel state = {}", mediaChannel.getState().name());
				if (mediaChannel.waitToReady() && mediaChannel.getState().isAvailable()) {
					log.debug("PARTICIPANT {}: Creating a subscriber endpoint to user {}", this.getUuid(),
							subscriberStreamId);
					log.info("uuid({}) 订阅 分发的uuid({}) targetPipeline {}  mediaChannel.publisher={}",
							this.getUuid(), sender.getUuid(), mediaChannel.getTargetPipeline(), mediaChannel.getPublisher().getEndpoint().getId());
					senderPublisher = mediaChannel.getPublisher();
					subscriberStreamId = this.getUuid() + "_receive_" + senderPublisher.getStreamId();
					subscriber = getNewAndCompareSubscriber(subscriberStreamId, mediaChannel.getTargetPipeline(), subscriber);
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
				log.warn("PARTICIPANT {}: Two threads are trying to create at " + "the same time a subscriber endpoint for user {}",
						this.getUuid(), subscriberStreamId);
				return null;
			}
			if (subscriber.getEndpoint() == null) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE, "Unable to create subscriber endpoint");
			}


			subscriber.setEndpointName(subscriberStreamId);
			subscriber.getEndpoint().setName(subscriberStreamId);
			subscriber.getEndpoint().addTag(strMSTagDebugEndpointName, getParticipantName() + "_sub_" + sender.getUuid() +
					"_" + streamType + "_cid_" + RandomStringUtils.randomAlphabetic(6) + "_stream_" + subscriber.getStreamId());
					//"_" + streamType + "_stream_" + subscriber.getStreamId());
			subscriber.setStreamId(subscriberStreamId);
			returnObj.put("subscribeId", subscriberStreamId);
			endpointConfig.addEndpointListeners(subscriber, "subscriber");
		} catch (OpenViduException e) {
            this.subscribers.remove(publishStreamId);
			throw e;
		}

        log.debug("PARTICIPANT {}: Created subscriber endpoint for user {}", this.getParticipantPublicId(), publishStreamId);
		try {
			String sdpAnswer = subscriber.subscribeVideo(sdpOffer, senderPublisher, streamMode);
            log.info("PARTICIPANT {}: Is now receiving video from {} in room {}", this.getParticipantPublicId(),
                    publishStreamId, this.session.getSessionId());

			if (!ProtocolElements.RECORDER_PARTICIPANT_PUBLICID.equals(this.getParticipantPublicId())) {
				endpointConfig.getCdr().recordNewSubscriber(this, this.session.getSessionId(),
						subscriberStreamId, sender.getParticipantPublicId(), subscriber.createdAt());
			}

			if (Objects.equals(session.getConferenceMode(), ConferenceModeEnum.MCU) &&
                    Objects.equals(StreamModeEnum.MIX_MAJOR, streamMode)) {
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
			this.subscribers.remove(subscriberStreamId);
			releaseSubscriberEndpoint(senderPublisher.getStreamId(), subscriber, null);
		}
		return null;
	}

    public String translateSubscribeId(String receiveUuid, String publishStreamId) {
        return receiveUuid + "_receive_" + publishStreamId;
        //return publishStreamId;
    }

	public void cancelReceivingMedia(String subscribeId, EndReason reason) {
		log.info("PARTICIPANT {}: cancel receiving media from {}", this.getParticipantPublicId(), subscribeId);
		SubscriberEndpoint subscriberEndpoint = subscribers.remove(subscribeId);

		if (subscriberEndpoint == null || subscriberEndpoint.getEndpoint() == null) {
			log.warn("PARTICIPANT {}: Trying to cancel receiving video from subscribeId {}. "
					+ "But there is no such subscriber endpoint.", this.getParticipantPublicId(), subscribeId);
		} else {
			releaseSubscriberEndpoint(subscribeId, subscriberEndpoint, reason);
			log.info("PARTICIPANT {}: stopped receiving media from {} in room {}", this.getParticipantPublicId(),
					subscribeId, this.session.getSessionId());
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
		releaseAllPublisherEndpoint(reason, kmsDisconnectionTime);
		if (session.isShare(this.getUuid()) &&
				!Objects.isNull(session.getCompositeService().getShareStreamId())) {
			session.getCompositeService().setShareStreamId(null);
		}
	}

	/**
	 * Returns a {@link SubscriberEndpoint} for the given participant public id. The
	 * endpoint is created if not found.
	 *
	 * @param subscribeId id of another user
	 * @return the endpoint instance
	 */
    public SubscriberEndpoint getNewOrExistingSubscriber(String subscribeId) {
        SubscriberEndpoint subscriberEndpoint = new SubscriberEndpoint(webParticipant, this, subscribeId,
                this.getPipeline(), this.session.getCompositeService(), this.openviduConfig);

        SubscriberEndpoint existingSendingEndpoint = this.subscribers.putIfAbsent(subscribeId, subscriberEndpoint);
        if (existingSendingEndpoint != null) {
            subscriberEndpoint = existingSendingEndpoint;
            log.trace("PARTICIPANT {}: Already exists a subscriber endpoint to user {}", this.getParticipantPublicId(),
                    subscribeId);
        } else {
            log.debug("PARTICIPANT {}: New subscriber endpoint to user {}", this.getParticipantPublicId(), subscribeId);
        }

        return subscriberEndpoint;
    }

	public SubscriberEndpoint getNewAndCompareSubscriber(String senderPublicId, MediaPipeline pipeline, SubscriberEndpoint compare) {
		SubscriberEndpoint subscriberEndpoint = new SubscriberEndpoint(webParticipant, this, senderPublicId,
				pipeline, this.session.getCompositeService(), this.openviduConfig);
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

//	2.0 delete
//	public void addIceCandidate(String endpointName, IceCandidate iceCandidate) {
//		if (this.getParticipantPublicId().equals(endpointName)) {
//			if (Objects.isNull(this.publisher)) {
//				// Initialize a PublisherEndpoint
//				this.publisher = new PublisherEndpoint(webParticipant, this, getParticipantPublicId(),
//						this.session.getPipeline(), this.openviduConfig);
//
//				this.publisher.setCompositeService(this.session.compositeService);
//			}
//			this.publisher.addIceCandidate(iceCandidate);
//		} else {
//			this.getNewOrExistingSubscriber(endpointName).addIceCandidate(iceCandidate);
//		}
//	}

	public ErrorCodeEnum addIceCandidate(String endpointName, IceCandidate iceCandidate) {
		if (!endpointName.contains("_receive_")) {
		//if (endpointName.startsWith(this.getUuid())) {
			Optional<PublisherEndpoint> find = this.publishers.values().stream().filter(ep -> ep.getStreamId().equals(endpointName)).findFirst();
			if (!find.isPresent()) {
				log.warn("can`t find endpointName {},{}", this.getUuid(), endpointName);
				return ErrorCodeEnum.ENP_POINT_NAME_NOT_EXIST;
			} else {
				find.get().addIceCandidate(iceCandidate);
			}
		} else {
			this.getNewOrExistingSubscriber(endpointName).addIceCandidate(iceCandidate);
		}
		return ErrorCodeEnum.SUCCESS;
	}

	public void sendIceCandidate(String senderUuid, String endpointName, IceCandidate candidate) {
		session.sendIceCandidate(this.getParticipantPrivateId(), senderUuid, endpointName, candidate);
	}

	public void sendMediaError(ErrorEvent event) {
		String desc = event.getType() + ": " + event.getDescription() + "(errCode=" + event.getErrorCode() + ")";
		log.warn("PARTICIPANT {}: Media error encountered: {}", getParticipantPublicId(), desc);
		session.sendMediaError(this.getParticipantPrivateId(), desc);
	}

	private void releaseAllPublisherEndpoint(EndReason reason, long kmsDisconnectionTime) {
		this.publishers.values().forEach(ep -> releasePublisherEndpoint(ep, reason, kmsDisconnectionTime));
	}
	private void releasePublisherEndpoint(PublisherEndpoint publisherEndpoint, EndReason reason, long kmsDisconnectionTime) {
		if (publisherEndpoint != null && publisherEndpoint.getEndpoint() != null) {
			// 释放分发资源
			if (!publisherEndpoint.getMediaChannels().isEmpty()) {
				log.info("release mediaChannels reason {}", reason);
				publisherEndpoint.getMediaChannels().values().forEach(MediaChannel::release);
				publisherEndpoint.getMediaChannels().clear();
			}

			if (this.openviduConfig.isLivingModuleEnabled()
					&& this.livingManager.sessionIsBeingLived(session.getSessionId())) {
				this.livingManager.stopOneIndividualStreamLiving(session, this.getPublisherStreamId(),
						kmsDisconnectionTime);
			}

			publisherEndpoint.unregisterErrorListeners();
			if (publisherEndpoint.kmsWebrtcStatsThread != null) {
				publisherEndpoint.kmsWebrtcStatsThread.cancel(true);
			}

			for (MediaElement el : publisherEndpoint.getMediaElements()) {
				releaseElement(getParticipantPublicId(), el);
				log.info("Release publisher self mediaElement and object id:{}", el.getId());
			}

			releaseElement(getParticipantPublicId(), publisherEndpoint.getEndpoint());
			//publisherEndpoint.closeAudioComposite();
			if (Objects.nonNull(publisherEndpoint.getSipCompositeHubPort())) {
				releaseElement(getParticipantPublicId(), publisherEndpoint.getSipCompositeHubPort());
				session.asyncUpdateSipComposite();
			}
			this.session.deregisterPublisher();
			//todo 2.0 part streaming status need update
			//setStreaming(false);
			//todo 2.0 part streaming status need update
			this.publishers.remove(publisherEndpoint.getStreamType());
		} else {
			log.warn("PARTICIPANT {}: Trying to release publisher endpoint but is null", getParticipantPublicId());
		}
	}

	private void releaseSubscriberEndpoint(String subscribeId, SubscriberEndpoint subscriber, EndReason reason) {
		if (subscriber != null) {

			subscriber.unregisterErrorListeners();
			if (subscriber.kmsWebrtcStatsThread != null) {
				subscriber.kmsWebrtcStatsThread.cancel(true);
			}

			releaseElement(subscribeId, subscriber.getEndpoint());
			endpointConfig.getCdr().stopSubscriber(this.getParticipantPublicId(), subscribeId,
					subscriber.getStreamId(), reason);
		} else {
			log.warn("PARTICIPANT {}: Trying to release subscriber endpoint for '{}' but is null",
					this.getParticipantPublicId(), subscribeId);
		}
	}

	public void releaseAllPublisherEndpoint() {
		for (PublisherEndpoint publisherEndpoint : this.getPublishers().values()) {
			this.releaseElement(publisherEndpoint.getStreamId(), publisherEndpoint.getEndpoint());
		}
	}

	public void releaseElement(final String endpointName, final MediaElement element) {
		if (Objects.isNull(element)) return;
		final String eid = element.getId();
		try {
			element.release(new Continuation<Void>() {
				@Override
				public void onSuccess(Void result) throws Exception {
					log.debug("PARTICIPANT {}: Released successfully media element #{} for {}",
							getParticipantPublicId(), eid, endpointName);
				}

				@Override
				public void onError(Throwable cause) throws Exception {
					log.warn("PARTICIPANT {}: Could not release media element #{} for {}", getParticipantPublicId(),
							eid, endpointName, cause);
				}
			});
		} catch (Exception e) {
			log.error("PARTICIPANT {}: Error calling release on elem #{} for {}", getParticipantPublicId(), eid,
					endpointName, e);
		}
	}

	public MediaPipeline getPipeline() {
		return this.session.getPipeline();
	}

	//todo 2.0 Deprecated
	@Deprecated
	@Override
	public String getPublisherStreamId() {
		//return publisher.getStreamId();
		return publishers.get(StreamType.MAJOR).getStreamId();
	}

	public void resetPublisherEndpoint() {
		log.info("Reseting publisher endpoint for participant {}", this.getParticipantPublicId());
        if (!OpenViduRole.NON_PUBLISH_ROLES.contains(this.getRole())) {
//            this.publisher = new PublisherEndpoint(webParticipant, this, this.getParticipantPublicId(),
//                    this.session.getPipeline(), this.openviduConfig);

            //this.publisher.setCompositeService(this.session.getCompositeService());
        }
	}

	public void notifyClient(String method, JsonObject param) {
		this.session.notifyClient(this.participantPrivateId, method, param);
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
		Set<Participant> participants = getSession().getParticipants();
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
						subscriberEndpoint.controlMediaTypeLink(MediaType.VIDEO, operation);
					}
				}
			});
		}
	}
// delete 2.0 @Deprecated
//	/**
//	 * pause or resume stream
//	 * @param targetPart Participant
//	 * @param operation on,off
//	 * @param mediaType video,audio
//	 * @param publicIds participants' publicId that this participant receive video or audio from
//	 */
//	void pauseAndResumeStreamInSession(Participant targetPart, OperationMode operation, String mediaType, Set<String> publicIds) {
//		if (publicIds.contains(targetPart.getParticipantPublicId())) {
//			log.info("PARTICIPANT {}: Is now {} receiving {} from {} in room {}",
//					this.getParticipantPublicId(), Objects.equals(operation, OperationMode.on) ?
//							"resume" : "pause", mediaType, this.getParticipantPublicId(), this.session.getSessionId());
//			KurentoParticipant kurentoParticipant = (KurentoParticipant) targetPart;
//			SubscriberEndpoint subscriberEndpoint = subscribers.get(targetPart.getParticipantPublicId());
//			if (Objects.nonNull(subscriberEndpoint)) {
//				subscriberEndpoint.controlMediaTypeLink(kurentoParticipant.getPublisher(), MediaType.valueOf(mediaType.toUpperCase()), Objects.equals(operation, OperationMode.on) ? VoiceMode.off : VoiceMode.on);
//			}
//		} else {
//			log.info("PARTICIPANT {}: Is not subscriber {} from {} in room {}",
//					this.getParticipantPublicId(), mediaType, this.getParticipantPublicId(), this.session.getSessionId());
//		}
//	}

	/**
	 * pause or resume stream
	 * @param operation on,off
	 * @param mediaType video,audio
	 */
	void pauseAndResumeStreamInSession(OperationMode operation, String mediaType, String subscribeId) {
		SubscriberEndpoint subscriberEndpoint = this.subscribers.get(subscribeId);
		if (Objects.nonNull(subscriberEndpoint)) {
			log.info("PARTICIPANT {}: Is now {} receiving {} from {} in room {}",
					this.getParticipantPublicId(), Objects.equals(operation, OperationMode.on) ?
							"resume" : "pause", mediaType, this.getParticipantPublicId(), this.session.getSessionId());
			subscriberEndpoint.controlMediaTypeLink(MediaType.valueOf(mediaType.toUpperCase()), Objects.equals(operation, OperationMode.on) ? VoiceMode.off : VoiceMode.on);
		} else {
			log.info("PARTICIPANT {}: Is not subscriber {} from {} in room {}",
					this.getParticipantPublicId(), mediaType, this.getParticipantPublicId(), this.session.getSessionId());
		}
	}

	public void notifyPublishChannelPass(PublisherEndpoint endpoint) {
		session.notifyPublishChannelPass(this, endpoint);
	}

}
