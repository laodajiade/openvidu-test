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

package io.openvidu.server.kurento.endpoint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.PushStreamStatusEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.enums.TerminalTypeEnum;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.MediaOptions;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.service.SessionEventRecord;
import io.openvidu.server.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.kurento.client.*;
import org.kurento.client.Properties;
import org.kurento.jsonrpc.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

/**
 * Publisher aspect of the {@link MediaEndpoint}.
 *
 * @author <a href="mailto:rvlad@naevatec.com">Radu Tom Vlad</a>
 */
public class PublisherEndpoint extends MediaEndpoint {

    private final static Logger log = LoggerFactory.getLogger(PublisherEndpoint.class);

    protected MediaOptions mediaOptions;

    @Getter
    @Setter
    private PassThrough passThru = null;
    private ListenerSubscription passThruSubscription = null;
    private Properties ptProperties = new Properties();

    private HubPort pubHubPort = null;
    private ListenerSubscription majorShareHubPortSubscription = null;
    private Properties pubHubProperties = new Properties();

    private HubPort recordHubPort = null;
    private HubPort liveHubPort = null;
    // delete 2.0
    //private HubPort sipCompositeHubPort = null;

    // Audio composite.
    // private Composite audioComposite = null;
//	private final Object audioCompositeCreateLock = new Object();
//	private final Object audioCompositeReleaseLock = new Object();

    private HubPort audioHubPortOut = null;

    @Getter
    private boolean streaming = false;

    private GenericMediaElement filter;
    private Map<String, Set<String>> subscribersToFilterEvents = new ConcurrentHashMap<>();
    private Map<String, ListenerSubscription> filterListeners = new ConcurrentHashMap<>();

    private Map<String, MediaElement> elements = new HashMap<>();
    private LinkedList<String> elementIds = new LinkedList<>();
//	private ConcurrentHashMap<String, String> connectToOthersHubportIns = new ConcurrentHashMap<>(50);
//	private ConcurrentHashMap<String, String> othersConToSelfHubportIns = new ConcurrentHashMap<>(50);

    private boolean connected = false;
    private boolean isChannelPassed = false;

    @Setter
    @Getter
    private PushStreamStatusEnum pushStreamStatus = PushStreamStatusEnum.on;

    @Getter
    private CountDownLatch publisherLatch = new CountDownLatch(0);

    @Getter
    private final StreamType streamType;
    @Getter
    private final ConcurrentMap<String, MediaChannel> mediaChannels = new ConcurrentHashMap<>();

    private Map<String, ListenerSubscription> elementsErrorSubscriptions = new HashMap<String, ListenerSubscription>();

    //delete 2.0 需要删除
//	@Deprecated
//	public PublisherEndpoint(boolean web, KurentoParticipant owner, String uuid, MediaPipeline pipeline,
//							 OpenviduConfig openviduConfig) {
//		super(web, owner, uuid, pipeline, openviduConfig, log);
//		this.streamType = StreamType.MAJOR;
//		this.endpointName = uuid + '_' + streamType + '_' + RandomStringUtils.randomAlphabetic(6);
//	}

    public PublisherEndpoint(boolean web, KurentoParticipant owner, String uuid, MediaPipeline pipeline, StreamType streamType,
                             OpenviduConfig openviduConfig) {
        super(web, owner, uuid, pipeline, openviduConfig, log);
        String streamId = uuid + '_' + streamType + "_" + RandomStringUtils.randomAlphabetic(6).toUpperCase();
        this.endpointName = streamId;
        setStreamId(streamId);
        this.streamType = streamType;
    }

    public void createEndpoint(String epTraceId) {
        epProperties.add("traceId", epTraceId);
        epProperties.add("createAt", String.valueOf(System.currentTimeMillis()));
        epProperties.add("trackAppType", streamType.name());
        epProperties.add("trackDirect", OpenViduRole.PUBLISHER.name());
        epProperties.add("userUuid", getOwner().getUuid());
        epProperties.add("userName", getOwner().getUsername());
        epProperties.add("userPhone", "");
        this.createEndpoint(this.createPublisherLatch());
    }

    @Override
    protected void internalEndpointInitialization(final CountDownLatch endpointLatch) {
        super.internalEndpointInitialization(endpointLatch);

        String epTraceId = String.valueOf(epProperties.get("traceId"));
        ptProperties.add("traceId", epTraceId.substring(0, epTraceId.length() - 3) + "pt");
        ptProperties.add("createAt", String.valueOf(System.currentTimeMillis()));
        passThru = new PassThrough.Builder(getPipeline()).withProperties(ptProperties).build();
        log.info("Pub EP create passThrough.");
        passThruSubscription = registerElemErrListener(passThru);

        KurentoSession kurentoSession = ((KurentoParticipant) this.getOwner()).getSession();
        if (kurentoSession.getConferenceMode().equals(ConferenceModeEnum.MCU)) {
            if (Objects.isNull(getCompositeService())) {
                setCompositeService(kurentoSession.getCompositeService());
            }
//            if (Objects.isNull(this.getPubHubPort())) {
//                createMajorShareHubPort(getMajorShareComposite());
//            }
        }
    }

    @Override
    public synchronized void unregisterErrorListeners() {
        super.unregisterErrorListeners();
        unregisterElementErrListener(passThru, passThruSubscription);
        unregisterElementErrListener(pubHubPort, majorShareHubPortSubscription);
        for (String elemId : elementIds) {
            unregisterElementErrListener(elements.get(elemId), elementsErrorSubscriptions.remove(elemId));
        }
        this.streaming = false;
    }

    @Override
    public void notifyEndpointPass(String typeOfEndpoint) {
        if (typeOfEndpoint.compareTo("publisher") == 0 && !isChannelPassed) {
            isChannelPassed = true;
            new Thread(() -> {
                KurentoParticipant kParticipant = (KurentoParticipant) this.getOwner();
                kParticipant.notifyPublishChannelPass(this);
            }).start();
        }
    }

//	public Composite getAudioComposite() { return this.audioComposite; }
//
//	public HubPort createHubPort(Composite composite) {
//		HubPort hubPort = new HubPort.Builder(composite).build();
//		elements.put(hubPort.getId(), hubPort);
//		return hubPort;
//	}

    public HubPort createRecordHubPort(Composite composite) {
        recordHubPort = new HubPort.Builder(composite).build();
        return recordHubPort;
    }

    public HubPort createLiveHubPort(Composite composite) {
        liveHubPort = new HubPort.Builder(composite).build();
        return liveHubPort;
    }

    // delete 2.0
//	public HubPort createSipCompositeHubPort(Composite composite) {
//		sipCompositeHubPort = new HubPort.Builder(composite).build();
//		sipCompositeHubPort.setMinOutputBitrate(2000000);
//		sipCompositeHubPort.setMaxOutputBitrate(2000000);
//		log.info("create sip composite {} hubport {}", composite.getName(), sipCompositeHubPort.getId());
//		sipCompositeHubPort.setName(this.getStreamId()+"_"+"hubPort");
//		return sipCompositeHubPort;
//	}

    public void connectAudioIn(MediaElement sink) {
//		internalSinkConnect(this.getEndpoint(), sink, MediaType.AUDIO);
    }

    public void connectAudioOut(MediaElement sink) {
        internalSinkConnect(pubHubPort, sink, MediaType.AUDIO);
    }

//	public void closeAudioComposite() {
//		synchronized (audioCompositeReleaseLock) {
//			if (Objects.isNull(audioComposite)) {
//				log.warn("audio composite already released.");
//				return;
//			}
//
//			// release others connect to self hub port in
//			KurentoParticipant kParticipant = (KurentoParticipant) this.getOwner();
//			KurentoSession kurentoSession= kParticipant.getSession();
//			othersConToSelfHubportIns.forEach((publicId, portId) -> {
//				Participant participant = kurentoSession.getParticipantByPublicId(publicId);
//				if (!Objects.isNull(participant)) {
//					KurentoParticipant kPart = (KurentoParticipant) participant;
//					kPart.releaseAllPublisherEndpoint();
//				}
//			});
//
//			audioHubPortOut.release();
//			log.info("Release Audio Composite HubPort Out and object id:{}", audioComposite.getId());
//
//			audioComposite.release();
//			log.info("Release Audio Composite and object id:{}", audioComposite.getId());
//		}
//	}

	/*public void innerConnectAudio() {
		KurentoParticipant kParticipant = (KurentoParticipant) this.getOwner();
		Set<Participant> participants = kParticipant.getSession().getParticipants();
		// latest participant
		Composite audioComposite = getAudioComposite();
		for (Participant p : participants) {
			KurentoParticipant p1 = (KurentoParticipant) p;
			if (Objects.equals(p1, kParticipant)) {
				continue;
			}

			if (!Objects.equals(p.getStreamType(), StreamType.SHARING) &&
					!OpenViduRole.NON_PUBLISH_ROLES.contains(p.getRole())) {
				if (!Objects.isNull(p1.getPublisher())) {
					HubPort hubPortIn = createHubPort(audioComposite);
					p1.getPublisher().connectAudioIn(hubPortIn);
					connectToOthersHubportIns.put(p1.getParticipantPublicId(), hubPortIn.getId());
				}
			}
		}

		// already exist participant
		for (Participant p : participants) {
			KurentoParticipant p1 = (KurentoParticipant) p;
			if (Objects.equals(p1, kParticipant)) {
				continue;
			}
			if (!Objects.equals(p.getStreamType(), StreamType.SHARING) &&
					!OpenViduRole.NON_PUBLISH_ROLES.contains(p.getRole())) {
				if (!Objects.isNull(p1.getPublisher())) {
					Composite existAudioComposite = p1.getPublisher().getAudioComposite();
					if (!Objects.isNull(existAudioComposite)) {
						HubPort hubPortIn = p1.getPublisher().createHubPort(existAudioComposite);
						connectAudioIn(hubPortIn);
						othersConToSelfHubportIns.put(p1.getParticipantPublicId(), hubPortIn.getId());
					}
				}
			}
		}
	}*/

    /**
     * @return all media elements created for this publisher, except for the main
     * element ( {@link WebRtcEndpoint})
     */
    public synchronized Collection<MediaElement> getMediaElements() {
        if (passThru != null) {
            elements.put(passThru.getId(), passThru);
        }
        if (pubHubPort != null) {
            elements.put(pubHubPort.getId(), pubHubPort);
        }
        if (audioHubPortOut != null) {
            elements.put(audioHubPortOut.getId(), audioHubPortOut);
        }
        return elements.values();
    }

    public GenericMediaElement getFilter() {
        return this.filter;
    }

    public boolean isListenerAddedToFilterEvent(String eventType) {
        return (this.subscribersToFilterEvents.containsKey(eventType) && this.filterListeners.containsKey(eventType));
    }

    public Set<String> getPartipantsListentingToFilterEvent(String eventType) {
        return this.subscribersToFilterEvents.get(eventType);
    }

    public boolean storeListener(String eventType, ListenerSubscription listener) {
        return (this.filterListeners.putIfAbsent(eventType, listener) == null);
    }

    public ListenerSubscription removeListener(String eventType) {
        // Clean all participant subscriptions to this event
        this.subscribersToFilterEvents.remove(eventType);
        // Clean ListenerSubscription object for this event
        return this.filterListeners.remove(eventType);
    }

    public void addParticipantAsListenerOfFilterEvent(String eventType, String participantPublicId) {
        this.subscribersToFilterEvents.putIfAbsent(eventType, new HashSet<>());
        this.subscribersToFilterEvents.get(eventType).add(participantPublicId);
    }

    public boolean removeParticipantAsListenerOfFilterEvent(String eventType, String participantPublicId) {
        if (!this.subscribersToFilterEvents.containsKey(eventType)) {
            String streamId = this.getStreamId();
            log.error("Request to removeFilterEventListener to stream {} gone wrong: Filter {} has no listener added",
                    streamId, eventType);
            throw new OpenViduException(Code.FILTER_EVENT_LISTENER_NOT_FOUND,
                    "Request to removeFilterEventListener to stream " + streamId + " gone wrong: Filter " + eventType
                            + " has no listener added");
        }
        this.subscribersToFilterEvents.computeIfPresent(eventType, (type, subs) -> {
            subs.remove(participantPublicId);
            return subs;
        });
        return this.subscribersToFilterEvents.get(eventType).isEmpty();
    }

    public void cleanAllFilterListeners() {
        for (String eventType : this.subscribersToFilterEvents.keySet()) {
            this.removeListener(eventType);
        }
    }

    /**
     * Initializes this media endpoint for publishing media and processes the SDP
     * offer or answer. If the internal endpoint is an {@link WebRtcEndpoint}, it
     * first registers an event listener for the ICE candidates and instructs the
     * endpoint to start gathering the candidates. If required, it connects to
     * itself (after applying the intermediate media elements and the
     * {@link PassThrough}) to allow loopback of the media stream.
     *
     * @param sdpType                indicates the type of the sdpString (offer or
     *                               answer)
     * @param sdpString              offer or answer from the remote peer
     * @param doLoopback             loopback flag
     * @param loopbackAlternativeSrc alternative loopback source
     * @param loopbackConnectionType how to connect the loopback source
     * @return the SDP response (the answer if processing an offer SDP, otherwise is
     * the updated offer generated previously by this endpoint)
     */
    public synchronized String publish(SdpType sdpType, String sdpString, boolean doLoopback,
                                       MediaElement loopbackAlternativeSrc, MediaType loopbackConnectionType) {
        registerOnIceCandidateEventListener(this.getOwner().getUuid());
        if (doLoopback) {
            if (loopbackAlternativeSrc == null) {
                connect(this.getEndpoint(), loopbackConnectionType);
            } else {
                connectAltLoopbackSrc(loopbackAlternativeSrc, loopbackConnectionType);
            }
        } else {
            innerConnect();
        }
        String sdpResponse;
        switch (sdpType) {
            case ANSWER:
                sdpResponse = processAnswer(sdpString);
                break;
            case OFFER:
                sdpResponse = processOffer(sdpString);
                break;
            default:
                throw new OpenViduException(Code.MEDIA_SDP_ERROR_CODE, "Sdp type not supported: " + sdpType);
        }
        this.createdAt = System.currentTimeMillis();
        this.streaming = true;
        return sdpResponse;
    }

    public synchronized String preparePublishConnection() {
        return generateOffer();
    }

    public synchronized void checkInnerConnect() {
        if (!connected) {
            innerConnect();
        }
    }

    public synchronized void connect(MediaElement sink) {
        if (!connected) {
            innerConnect();
        }
        internalSinkConnect(passThru, sink);
    }

    public synchronized void connect(MediaElement sink, MediaType type) {
        if (!connected) {
            innerConnect();
        }
        internalSinkConnect(passThru, sink, type);
    }

    public synchronized void disconnectFrom(MediaElement sink) {
        internalSinkDisconnect(passThru, sink);
        internalSinkDisconnect(pubHubPort, sink);
    }

    public synchronized void disconnectFrom(MediaElement sink, MediaType type) {
        internalSinkDisconnect(passThru, sink, type);
        internalSinkDisconnect(pubHubPort, sink, type);
    }

    public synchronized void sfuDisconnectFrom(MediaElement sink, MediaType type) {
        internalSinkDisconnect(passThru, sink, type);
    }

    public void connectRecordHubPort(HubPort hubPort) {
        internalSinkConnect(this.getEndpoint(), hubPort);
    }

    public void disconnectRecordHubPort(HubPort hubPort) {
        internalSinkDisconnect(this.getEndpoint(), hubPort);
    }


    /**
     * Changes the media passing through a chain of media elements by applying the
     * specified element/shaper. The element is plugged into the stream only if the
     * chain has been initialized (a.k.a. media streaming has started), otherwise it
     * is left ready for when the connections between elements will materialize and
     * the streaming begins.
     *
     * @param shaper {@link MediaElement} that will be linked to the end of the
     *               chain (e.g. a filter)
     * @return the element's id
     * @throws OpenViduException if thrown, the media element was not added
     */
    public String apply(GenericMediaElement shaper) throws OpenViduException {
        return apply(shaper, null);
    }

    /**
     * Same as {@link (MediaElement)}, can specify the media type that will be
     * streamed through the shaper element.
     *
     * @param shaper {@link MediaElement} that will be linked to the end of the
     *               chain (e.g. a filter)
     * @param type   indicates which type of media will be connected to the shaper
     *               ({@link MediaType}), if null then the connection is mixed
     * @return the element's id
     * @throws OpenViduException if thrown, the media element was not added
     */
    public synchronized String apply(GenericMediaElement shaper, MediaType type) throws OpenViduException {
        String id = shaper.getId();
        if (id == null) {
            throw new OpenViduException(Code.MEDIA_WEBRTC_ENDPOINT_ERROR_CODE,
                    "Unable to connect media element with null id");
        }
        if (elements.containsKey(id)) {
            throw new OpenViduException(Code.MEDIA_WEBRTC_ENDPOINT_ERROR_CODE,
                    "This endpoint already has a media element with id " + id);
        }
        MediaElement first = null;
        if (!elementIds.isEmpty()) {
            first = elements.get(elementIds.getFirst());
        }
        if (connected) {
            if (first != null) {
                internalSinkConnect(first, shaper, type);
            } else {
                internalSinkConnect(this.getEndpoint(), shaper, type);
            }
            internalSinkConnect(shaper, passThru, type);
            internalSinkConnect(shaper, pubHubPort, type);
        }
        elementIds.addFirst(id);
        elements.put(id, shaper);

        this.filter = shaper;

        elementsErrorSubscriptions.put(id, registerElemErrListener(shaper));
        return id;
    }

    /**
     * Removes the media element object found from the media chain structure. The
     * object is released. If the chain is connected, both adjacent remaining
     * elements will be interconnected.
     *
     * @param shaper {@link MediaElement} that will be removed from the chain
     * @throws OpenViduException if thrown, the media element was not removed
     */
    public synchronized void revert(MediaElement shaper) throws OpenViduException {
        revert(shaper, true);
    }

    public synchronized void revert(MediaElement shaper, boolean releaseElement) throws OpenViduException {
        final String elementId = shaper.getId();
        if (!elements.containsKey(elementId)) {
            throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
                    "This endpoint (" + getEndpointName() + ") has no media element with id " + elementId);
        }

        MediaElement element = elements.remove(elementId);
        unregisterElementErrListener(element, elementsErrorSubscriptions.remove(elementId));

        // careful, the order in the elems list is reverted
        if (connected) {
            String nextId = getNext(elementId);
            String prevId = getPrevious(elementId);
            // next connects to prev
            MediaElement prev = null;
            MediaElement next = null;
            if (nextId != null) {
                next = elements.get(nextId);
            } else {
                next = this.getEndpoint();
            }
            if (prevId != null) {
                prev = elements.get(prevId);
            } else {
                prev = passThru;
            }
            internalSinkConnect(next, prev);
        }
        elementIds.remove(elementId);
        if (releaseElement) {
            element.release(new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.trace("EP {}: Released media element {}", getEndpointName(), elementId);
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.error("EP {}: Failed to release media element {}", getEndpointName(), elementId, cause);
                }
            });
        }
        this.filter = null;
    }

    public JsonElement execMethod(String method, JsonObject params) throws OpenViduException {
        Props props = new JsonUtils().fromJsonObjectToProps(params);
        return (JsonElement) ((GenericMediaElement) this.filter).invoke(method, props);
    }

	/*public synchronized void mute(TrackType muteType) {
		MediaElement sink = passThru;
		if (!elements.isEmpty()) {
			String sinkId = elementIds.peekLast();
			if (!elements.containsKey(sinkId)) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
						"This endpoint (" + getEndpointName() + ") has no media element with id " + sinkId
								+ " (should've been connected to the internal ep)");
			}
			sink = elements.get(sinkId);
		} else {
			log.debug("Will mute connection of WebRTC and PassThrough (no other elems)");
		}
		switch (muteType) {
		case ALL:
			internalSinkDisconnect(this.getEndpoint(), sink);
			break;
		case AUDIO:
			internalSinkDisconnect(this.getEndpoint(), sink, MediaType.AUDIO);
			break;
		case VIDEO:
			internalSinkDisconnect(this.getEndpoint(), sink, MediaType.VIDEO);
			break;
		}
	}*/

	/*public synchronized void unmute(TrackType muteType) {
		MediaElement sink = passThru;
		if (!elements.isEmpty()) {
			String sinkId = elementIds.peekLast();
			if (!elements.containsKey(sinkId)) {
				throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
						"This endpoint (" + getEndpointName() + ") has no media element with id " + sinkId
								+ " (should've been connected to the internal ep)");
			}
			sink = elements.get(sinkId);
		} else {
			log.debug("Will unmute connection of WebRTC and PassThrough (no other elems)");
		}
		switch (muteType) {
		case ALL:
			internalSinkConnect(this.getEndpoint(), sink);
			break;
		case AUDIO:
			internalSinkConnect(this.getEndpoint(), sink, MediaType.AUDIO);
			break;
		case VIDEO:
			internalSinkConnect(this.getEndpoint(), sink, MediaType.VIDEO);
			break;
		}
	}*/

    private String getNext(String uid) {
        int idx = elementIds.indexOf(uid);
        if (idx < 0 || idx + 1 == elementIds.size()) {
            return null;
        }
        return elementIds.get(idx + 1);
    }

    private String getPrevious(String uid) {
        int idx = elementIds.indexOf(uid);
        if (idx <= 0) {
            return null;
        }
        return elementIds.get(idx - 1);
    }

    private void connectAltLoopbackSrc(MediaElement loopbackAlternativeSrc, MediaType loopbackConnectionType) {
        if (!connected) {
            innerConnect();
        }
        internalSinkConnect(loopbackAlternativeSrc, this.getEndpoint(), loopbackConnectionType);
    }

    private void innerConnect() {
        if (this.getEndpoint() == null) {
            throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
                    "Can't connect null endpoint (ep: " + getEndpointName() + ")");
        }
        MediaElement current = this.getEndpoint();
        String prevId = elementIds.peekLast();
        while (prevId != null) {
            MediaElement prev = elements.get(prevId);
            if (prev == null) {
                throw new OpenViduException(Code.MEDIA_ENDPOINT_ERROR_CODE,
                        "No media element with id " + prevId + " (ep: " + getEndpointName() + ")");
            }
            internalSinkConnect(current, prev);
            current = prev;
            prevId = getPrevious(prevId);
        }

        KurentoParticipant kurentoParticipant = (KurentoParticipant) getOwner();
        internalSinkConnect(current, passThru);

        if (kurentoParticipant.getSession().getConferenceMode().equals(ConferenceModeEnum.MCU)) {
            // todo yy 注释
            //internalSinkConnect(current, createMajorShareHubPort(this.getMajorShareComposite()));
            if (TerminalTypeEnum.S == kurentoParticipant.getTerminalType()) {
                // change the link order and unify the capability(send recv) of both two points
                // internalSinkConnect(pubHubPort, current, MediaType.VIDEO);
                log.info("sip terminal:{} published {} connected to sip", this.getOwner().getUuid(), this.getEndpointName());
                getCompositeService().connectSip(this);
            }
        }

        connected = true;
    }

    private void internalSinkConnect(final MediaElement source, final MediaElement sink) {
        source.connect(sink, new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.debug("EP {}: Elements have been connected (source {} -> sink {})", getEndpointName(),
                        source.getId(), sink.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("EP {}: Failed to connect media elements (source {} -> sink {})", getEndpointName(),
                        source.getId(), sink.getId(), cause);
            }
        });
    }

    /**
     * Same as {@link #internalSinkConnect(MediaElement, MediaElement)}, but can
     * specify the type of the media that will be streamed.
     *
     * @param source
     * @param sink
     * @param type   if null,
     *               {@link #internalSinkConnect(MediaElement, MediaElement)} will
     *               be used instead
     * @see #internalSinkConnect(MediaElement, MediaElement)
     */
    private void internalSinkConnect(final MediaElement source, final MediaElement sink, final MediaType type) {
        if (type == null) {
            internalSinkConnect(source, sink);
        } else {
            source.connect(sink, type, new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.debug("EP {}: {} media elements have been connected (source {} -> sink {})", getEndpointName(),
                            type, source.getId(), sink.getId());
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.warn("EP {}: Failed to connect {} media elements (source {} -> sink {})", getEndpointName(),
                            type, source.getId(), sink.getId(), cause);
                }
            });
        }
    }

    private void internalSinkDisconnect(final MediaElement source, final MediaElement sink) {
        source.disconnect(sink, new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) throws Exception {
                log.debug("EP {}: Elements have been disconnected (source {} -> sink {})", getEndpointName(),
                        source.getId(), sink.getId());
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("EP {}: Failed to disconnect media elements (source {} -> sink {})", getEndpointName(),
                        source.getId(), sink.getId(), cause);
            }
        });
    }

    /**
     * Same as {@link #internalSinkDisconnect(MediaElement, MediaElement)}, but can
     * specify the type of the media that will be disconnected.
     *
     * @param source
     * @param sink
     * @param type   if null,
     *               {@link #internalSinkConnect(MediaElement, MediaElement)} will
     *               be used instead
     * @see #internalSinkConnect(MediaElement, MediaElement)
     */
    private void internalSinkDisconnect(final MediaElement source, final MediaElement sink, final MediaType type) {
        if (type == null) {
            internalSinkDisconnect(source, sink);
        } else {
            source.disconnect(sink, type, new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) throws Exception {
                    log.info("EP {}: {} media elements have been disconnected (source {} -> sink {})",
                            getEndpointName(), type, source.getId(), sink.getId());
                }

                @Override
                public void onError(Throwable cause) throws Exception {
                    log.info("EP {}: Failed to disconnect {} media elements (source {} -> sink {})", getEndpointName(),
                            type, source.getId(), sink.getId(), cause);
                }
            });
        }
    }

    public CountDownLatch createPublisherLatch() {
        this.publisherLatch = new CountDownLatch(1);
        return getPublisherLatch();
    }

    public MediaOptions getMediaOptions() {
        return mediaOptions;
    }

    public void setMediaOptions(MediaOptions mediaOptions) {
        this.mediaOptions = mediaOptions;
    }

//	public HubPort getMajorHubPort() {
//		return majorHubPort;
//	}

    public synchronized HubPort createMajorShareHubPort(Composite composite) {
        if (this.pubHubPort != null) {
            return this.pubHubPort;
        }
        String epTraceId = String.valueOf(epProperties.get("traceId"));
        // format: "{roomId}_{senderId}_mixHubIn}"
        pubHubProperties.add("traceId", epTraceId.substring(0, epTraceId.length() - 3) + "mixHubIn");
        pubHubProperties.add("createAt", String.valueOf(System.currentTimeMillis()));
//        pubHubProperties.add("sdOsd", "testOsd");
        pubHubPort = new HubPort.Builder(composite).withProperties(pubHubProperties).build();
        SessionEventRecord.other(this.getOwner().getSessionId(), "createPubHubPort", this.getStreamId(), pubHubPort.getName());
        log.info("{} Pub EP create majorShareHubPort. {}", this.streamId, pubHubPort.getName());
        majorShareHubPortSubscription = registerElemErrListener(pubHubPort);
        return pubHubPort;
    }

    public HubPort getPubHubPort() {
        return pubHubPort;
    }

    public void releaseMajorShareHubPort() {
        if (pubHubPort != null) {
            pubHubPort.release();
        }
        pubHubPort = null;
    }

    public HubPort getRecordHubPort() {
        return recordHubPort;
    }

    public HubPort getLiveHubPort() {
        return liveHubPort;
    }

    // delete 2.0
//	public HubPort getSipCompositeHubPort() {
//		return sipCompositeHubPort;
//	}

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("streamId", this.getStreamId());
        json.addProperty("pipeline", this.getPipeline().getId());
        json.add("mediaOptions", this.mediaOptions.toJson());
        return json;
    }

    @Override
    public JsonObject withStatsToJson() {
        JsonObject json = super.withStatsToJson();
        JsonObject toJson = this.toJson();
        for (Entry<String, JsonElement> entry : toJson.entrySet()) {
            json.add(entry.getKey(), entry.getValue());
        }
        return json;
    }

    public String filterCollectionsToString() {
        return "{filter: " + ((this.filter != null) ? this.filter.getName() : "null") + ", listener: "
                + this.filterListeners.toString() + ", subscribers: " + this.subscribersToFilterEvents.toString() + "}";
    }

    private MediaElement getMediaElementById(String id) {
        return elements.get(id);
    }

}
