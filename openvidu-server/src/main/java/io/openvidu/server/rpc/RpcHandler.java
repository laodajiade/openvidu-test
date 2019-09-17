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

package io.openvidu.server.rpc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import com.google.gson.*;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.UserMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.pojo.User;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.internal.ws.WebSocketServerSession;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.MediaOptions;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.utils.GeoLocation;
import io.openvidu.server.utils.GeoLocationByIp;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class RpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory.getLogger(RpcHandler.class);

	private static final Gson gson = new GsonBuilder().create();

	@Autowired
	OpenviduConfig openviduConfig;

	@Autowired
	GeoLocationByIp geoLocationByIp;

	@Autowired
	SessionManager sessionManager;

	@Autowired
	RpcNotificationService notificationService;

	@Autowired
	CacheManage cacheManage;

	@Resource
	UserMapper userMapper;

	private ConcurrentMap<String, Boolean> webSocketEOFTransportError = new ConcurrentHashMap<>();

	@Override
	public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {

		String participantPrivateId = null;
		try {
			participantPrivateId = transaction.getSession().getSessionId();
		} catch (Throwable e) {
			log.error("Error getting WebSocket session ID from transaction {}", transaction, e);
			throw e;
		}

		log.debug("WebSocket session #{} - Request: {}", participantPrivateId, request);

		RpcConnection rpcConnection;
		if (ProtocolElements.ACCESS_IN_METHOD.equals(request.getMethod())) {
//		if (ProtocolElements.JOINROOM_METHOD.equals(request.getMethod())) {  // modified at 2019-09-12
			// Store new RpcConnection information if method 'joinRoom'
			rpcConnection = notificationService.newRpcConnection(transaction, request);
		} else if (notificationService.getRpcConnection(participantPrivateId) == null) {
			// Throw exception if any method is called before 'joinRoom'
			log.warn(
					"No connection found for participant with privateId {} when trying to execute method '{}'. Method 'Session.connect()' must be the first operation called in any session",
					participantPrivateId, request.getMethod());
			throw new OpenViduException(Code.TRANSPORT_ERROR_CODE,
					"No connection found for participant with privateId " + participantPrivateId
							+ ". Method 'Session.connect()' must be the first operation called in any session");
		}
		rpcConnection = notificationService.addTransaction(transaction, request);

		String sessionId = rpcConnection.getSessionId();
		if (sessionId == null && !ProtocolElements.FILTERS.contains(request.getMethod())) {
//		if (sessionId == null && !ProtocolElements.JOINROOM_METHOD.equals(request.getMethod())) {	// modified at 2019-09-12
			log.warn(
					"No session information found for participant with privateId {} when trying to execute method '{}'. Method 'Session.connect()' must be the first operation called in any session",
					participantPrivateId, request.getMethod());
			throw new OpenViduException(Code.TRANSPORT_ERROR_CODE,
					"No session information found for participant with privateId " + participantPrivateId
							+ ". Method 'Session.connect()' must be the first operation called in any session");
		}

		transaction.startAsync();

		switch (request.getMethod()) {
            case ProtocolElements.ACCESS_IN_METHOD:
                accessIn(rpcConnection, request);
                break;
            case ProtocolElements.ACCESS_OUT_METHOD:
                accessOut(rpcConnection, request);
				break;
			case ProtocolElements.CREATE_ROOM_METHOD:
				createRoom(rpcConnection, request);
				break;
			case ProtocolElements.SHARE_SCREEN_METHOD:
				shareScreen(rpcConnection, request);
				break;
			case ProtocolElements.STOP_SHARE_SCREEN_METHOD:
				stopShareScreen(rpcConnection, request);
				break;
			case ProtocolElements.GET_PARTICIPANTS_METHOD:
				getParticipants(rpcConnection, request);
				break;
			case ProtocolElements.SET_AUDIO_STATUS_METHOD:
				setAudioStatus(rpcConnection, request);
				break;
			case ProtocolElements.SET_VIDEO_STATUS_METHOD:
				setVideoStatus(rpcConnection, request);
				break;
            case ProtocolElements.RAISE_HAND_METHOD:
                raiseHand(rpcConnection, request);
                break;
			case ProtocolElements.PUT_DOWN_HAND_METHOD:
				putDownHand(rpcConnection, request);
				break;
			case ProtocolElements.LOCK_SESSION_METHOD:
				lockSession(rpcConnection, request);
				break;
			case ProtocolElements.JOINROOM_METHOD:
				joinRoom(rpcConnection, request);
				break;
			case ProtocolElements.LEAVEROOM_METHOD:
				leaveRoom(rpcConnection, request);
				break;
			case ProtocolElements.PUBLISHVIDEO_METHOD:
				publishVideo(rpcConnection, request);
				break;
			case ProtocolElements.ONICECANDIDATE_METHOD:
				onIceCandidate(rpcConnection, request);
				break;
			case ProtocolElements.RECEIVEVIDEO_METHOD:
				receiveVideoFrom(rpcConnection, request);
				break;
			case ProtocolElements.UNSUBSCRIBEFROMVIDEO_METHOD:
				unsubscribeFromVideo(rpcConnection, request);
				break;
			case ProtocolElements.SENDMESSAGE_ROOM_METHOD:
				sendMessage(rpcConnection, request);
				break;
			case ProtocolElements.UNPUBLISHVIDEO_METHOD:
				unpublishVideo(rpcConnection, request);
				break;
			case ProtocolElements.STREAMPROPERTYCHANGED_METHOD:
				streamPropertyChanged(rpcConnection, request);
				break;
			case ProtocolElements.FORCEDISCONNECT_METHOD:
				forceDisconnect(rpcConnection, request);
				break;
			case ProtocolElements.FORCEUNPUBLISH_METHOD:
				forceUnpublish(rpcConnection, request);
				break;
			case ProtocolElements.APPLYFILTER_METHOD:
				applyFilter(rpcConnection, request);
				break;
			case ProtocolElements.EXECFILTERMETHOD_METHOD:
				execFilterMethod(rpcConnection, request);
				break;
			case ProtocolElements.REMOVEFILTER_METHOD:
				removeFilter(rpcConnection, request);
				break;
			case ProtocolElements.ADDFILTEREVENTLISTENER_METHOD:
				addFilterEventListener(rpcConnection, request);
				break;
			case ProtocolElements.REMOVEFILTEREVENTLISTENER_METHOD:
				removeFilterEventListener(rpcConnection, request);
				break;
			default:
				log.error("Unrecognized request {}", request);
				break;
		}
	}

	private void accessIn(RpcConnection rpcConnection, Request<JsonObject> request) {
	    String userId = getStringParam(request, ProtocolElements.ACCESS_IN_USER_ID_PARAM);
	    String token = getStringParam(request, ProtocolElements.ACCESS_IN_TOKEN_PARAM);
	    // verify parameters
	    if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(token)) {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
			return;
		}

	    if (cacheManage.accessTokenEverValid(userId, token)) {
			notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
		} else {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.TOKEN_INVALID);
		}

    }

    private void accessOut(RpcConnection rpcConnection, Request<JsonObject> request) {
	    sessionManager.accessOut(rpcConnection);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

	private void createRoom(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.CREATE_ROOM_ID_PARAM);
		if (sessionManager.isNewSessionIdValid(sessionId)) {
			JsonObject respJson = new JsonObject();
			respJson.addProperty(ProtocolElements.CREATE_ROOM_ID_PARAM, sessionId);
			notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
		} else {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
		}
	}

	private void shareScreen(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.SHARE_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.SHARE_SOURCE_ID_PARAM);
		if (sessionManager.isSessionIdValid(sessionId)) {
			if (!sessionManager.isConflictSharing(sessionId, sourceId)) {
				notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
			} else {
				notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
						null, ErrorCodeEnum.SHARING_ALREADY_EXIST);
			}
		} else {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
		}
	}

	private void stopShareScreen(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.STOP_SHARE_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.STOP_SHARE_SOURCE_ID_PARAM);
		if (sessionManager.isSessionIdValid(sessionId)) {
			ConcurrentHashMap<String, String> sessionInfo = sessionManager.getSessionInfo(sessionId);
			if (Objects.equals(sourceId, sessionInfo.get("sharingSourceId"))) {
				notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
			} else {
				notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
						null, ErrorCodeEnum.PERMISSION_LIMITED);
			}
		} else {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.CONFERENCE_NOT_EXIST);
		}
	}

	private void getParticipants(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.GET_PARTICIPANTS_ROOM_ID_PARAM);
		JsonObject respJson = new JsonObject();
		JsonArray jsonArray = new JsonArray();
		List<Long> userIds = new ArrayList<>();
		sessionManager.getParticipants(sessionId).forEach(s -> userIds.add(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData").getAsLong()));
		if (!CollectionUtils.isEmpty(userIds)) {
			List<User> userList = userMapper.selectByPrimaryKeys(userIds);
			userList.forEach(user -> {
				JsonObject userObj = new JsonObject();
				userObj.addProperty("userId", user.getId());
				userObj.addProperty("account", user.getUsername());
				userObj.addProperty("role", sessionManager.getParticipants(sessionId).stream().filter(s ->
						user.getId().compareTo(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData").getAsLong()) == 0)
						.findFirst().get().getRole().name());
				jsonArray.add(userObj);
			});
		}
		respJson.add("participantList", jsonArray);
		notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
	}

	private void setAudioStatus(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.SET_AUDIO_ROOM_ID_PARAM);
		String targetId = getStringParam(request, ProtocolElements.SET_AUDIO_TARGET_ID_PARAM);

		String targetPrivateId = sessionManager.getParticipants(sessionId).stream().filter(s ->
				targetId.equals(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData").getAsString())).findFirst().get().getParticipantPrivateId();

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.SET_AUDIO_ROOM_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM, getStringParam(request, ProtocolElements.SET_AUDIO_SOURCE_ID_PARAM));
		params.addProperty(ProtocolElements.SET_AUDIO_TARGET_ID_PARAM, targetId);
		params.addProperty(ProtocolElements.SET_AUDIO_STATUS_PARAM, getStringParam(request, ProtocolElements.SET_AUDIO_STATUS_PARAM));
		this.notificationService.sendNotification(targetPrivateId, ProtocolElements.SET_AUDIO_STATUS_METHOD, params);
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void setVideoStatus(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.SET_VIDEO_ROOM_ID_PARAM);
		String targetId = getStringParam(request, ProtocolElements.SET_VIDEO_TARGET_ID_PARAM);

		JsonObject params = new JsonObject();
		String targetPrivateId = sessionManager.getParticipants(sessionId).stream().filter(s ->
				targetId.equals(gson.fromJson(s.getClientMetadata(), JsonObject.class).get("clientData").getAsString())).findFirst().get().getParticipantPrivateId();

		params.addProperty(ProtocolElements.SET_VIDEO_ROOM_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.SET_VIDEO_SOURCE_ID_PARAM, getStringParam(request, ProtocolElements.SET_VIDEO_SOURCE_ID_PARAM));
		params.addProperty(ProtocolElements.SET_VIDEO_TARGET_ID_PARAM, targetId);
		params.addProperty(ProtocolElements.SET_VIDEO_STATUS_PARAM, getStringParam(request, ProtocolElements.SET_VIDEO_STATUS_PARAM));
		this.notificationService.sendNotification(targetPrivateId, ProtocolElements.SET_VIDEO_STATUS_METHOD, params);
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

    private void raiseHand(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.RAISE_HAND_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.RAISE_HAND_SOURCE_ID_PARAM);
		sessionManager.getParticipant(rpcConnection.getParticipantPrivateId()).setHandStatus(ParticipantHandStatus.up);

		List<String> notifyClientPrivateIds = sessionManager.getParticipants(sessionId)
				.stream().map(p -> p.getParticipantPrivateId()).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(notifyClientPrivateIds)) {
			JsonObject params = new JsonObject();
			params.addProperty("roomId", sessionId);
			params.addProperty("sourceId", sourceId);
			notifyClientPrivateIds.forEach(client -> this.notificationService.sendNotification(client, ProtocolElements.RAISE_HAND_METHOD, params));
		}
		notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }

	private void putDownHand(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.PUT_DOWN_HAND_ROOM_ID_PARAM);
		String sourceId = getStringParam(request, ProtocolElements.PUT_DOWN_HAND_SOURCE_ID_PARAM);
		String targetId = request.getParams().has(ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM) ?
				request.getParams().get(ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM).getAsString() : null;
		Set<Participant> participants = sessionManager.getParticipants(sessionId);
		if (!StringUtils.isEmpty(targetId)) {
			participants.stream().filter(part ->
					targetId.equals(gson.fromJson(part.getClientMetadata(), JsonObject.class).get("clientData").getAsString()))
					.findFirst().get().setHandStatus(ParticipantHandStatus.down);
		} else {
			participants.forEach(part -> part.setHandStatus(ParticipantHandStatus.down));
		}

		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.PUT_DOWN_HAND_ROOM_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.PUT_DOWN_HAND_SOURCE_ID_PARAM, sourceId);
		if (!StringUtils.isEmpty(targetId)) params.addProperty(ProtocolElements.PUT_DOWN_HAND_TARGET_ID_PARAM, targetId);
		participants.forEach(participant ->
				this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.PUT_DOWN_HAND_METHOD, params));

		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
	}

	private void lockSession(RpcConnection rpcConnection, Request<JsonObject> request) {
		String sessionId = getStringParam(request, ProtocolElements.LOCK_SESSION_ROOM_ID_PARAM);
		if (sessionManager.getSession(sessionId).isClosed()) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), null, ErrorCodeEnum.CONFERENCE_IS_LOCKED);
			return;
		}
		if (sessionManager.getParticipant(rpcConnection.getParticipantPrivateId()).getRole() != OpenViduRole.MODERATOR) {
			this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(), null, ErrorCodeEnum.PERMISSION_LIMITED);
			return;
		}

		if (sessionManager.getSession(sessionId).setLocking(true)) {
			JsonObject params = new JsonObject();
			params.addProperty(ProtocolElements.LOCK_SESSION_ROOM_ID_PARAM, sessionId);

			sessionManager.getParticipants(sessionId).forEach(participant ->
					this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.PUT_DOWN_HAND_METHOD, params));
		}
		this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

	}

    public void joinRoom(RpcConnection rpcConnection, Request<JsonObject> request) {

		String sessionId = getStringParam(request, ProtocolElements.JOINROOM_ROOM_PARAM);
//		String token = getStringParam(request, ProtocolElements.JOINROOM_TOKEN_PARAM);
        String clientMetadata = getStringParam(request, ProtocolElements.JOINROOM_METADATA_PARAM);
        String role = getStringParam(request, ProtocolElements.JOINROOM_ROLE_PARAM);
		String secret = getStringParam(request, ProtocolElements.JOINROOM_SECRET_PARAM);
		String platform = getStringParam(request, ProtocolElements.JOINROOM_PLATFORM_PARAM);
		String participantPrivatetId = rpcConnection.getParticipantPrivateId();

		InetAddress remoteAddress = null;
		GeoLocation location = null;
		Object obj = rpcConnection.getSession().getAttributes().get("remoteAddress");
		if (obj != null && obj instanceof InetAddress) {
			remoteAddress = (InetAddress) obj;
			try {
				location = this.geoLocationByIp.getLocationByIp(remoteAddress);
			} catch (IOException e) {
				e.printStackTrace();
				location = null;
			} catch (Exception e) {
				log.warn("Error getting address location: {}", e.getMessage());
				location = null;
			}
		}

		HttpSession httpSession = (HttpSession) rpcConnection.getSession().getAttributes().get("httpSession");

		JsonObject sessions = (JsonObject) httpSession.getAttribute("openviduSessions");
		if (sessions == null) {
			// First time this final user connects to an OpenVidu session in this active
			// WebSocketSession. This is a new final user connecting to OpenVidu Server
			JsonObject json = new JsonObject();
			json.addProperty(sessionId, System.currentTimeMillis());
			httpSession.setAttribute("openviduSessions", json);
		} else {
			// This final user has already been connected to an OpenVidu session in this
			// active WebSocketSession
			if (sessions.has(sessionId)) {
				if (sessionManager.getSession(sessionId) != null) {
					// The previously existing final user is reconnecting to an OpenVidu session
					log.info("Final user reconnecting");
				} else if (sessionManager.getSessionNotActive(sessionId) != null) {
					// The previously existing final user is the first one connecting to a new
					// OpenVidu session that shares a sessionId with a previously closed session
					// (same customSessionId)
					sessions.addProperty(sessionId, System.currentTimeMillis());
				}
			} else {
				// The previously existing final user is connecting to a new session
				sessions.addProperty(sessionId, System.currentTimeMillis());
			}
		}

		boolean recorder = false;

		try {
			recorder = getBooleanParam(request, ProtocolElements.JOINROOM_RECORDER_PARAM);
		} catch (RuntimeException e) {
			// Nothing happens. 'recorder' param to false
		}

		boolean generateRecorderParticipant = false;

		if (openviduConfig.isOpenViduSecret(secret)) {
			sessionManager.newInsecureParticipant(participantPrivatetId);
//			token = RandomStringUtils.randomAlphanumeric(16).toLowerCase();
			if (recorder) {
				generateRecorderParticipant = true;
			}
		}

//		if (sessionManager.isTokenValidInSession(token, sessionId, participantPrivatetId)) {

//			String clientMetadata = getStringParam(request, ProtocolElements.JOINROOM_METADATA_PARAM);
        sessionManager.recordParticipantBySessionId(sessionId);
        if (sessionManager.formatChecker.isServerMetadataFormatCorrect(clientMetadata)) {

//            Token tokenObj = sessionManager.consumeToken(sessionId, participantPrivatetId, token);
            Participant participant;

            if (generateRecorderParticipant) {
                participant = sessionManager.newRecorderParticipant(sessionId, participantPrivatetId, clientMetadata, role);
            } else {
                participant = sessionManager.newParticipant(sessionId, participantPrivatetId, clientMetadata, role, location, platform,
                        httpSession.getId().substring(0, Math.min(16, httpSession.getId().length())));
            }

            rpcConnection.setSessionId(sessionId);
            sessionManager.joinRoom(participant, sessionId, request.getId());

        } else {
            log.error("ERROR: Metadata format set in client-side is incorrect");
            throw new OpenViduException(Code.USER_METADATA_FORMAT_INVALID_ERROR_CODE,
                    "Unable to join room. The metadata received from the client-side has an invalid format");
        }
		/*} else {
			log.error("ERROR: sessionId or token not valid");
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to join room. The user is not authorized");
		}*/
	}

	private void leaveRoom(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "disconnect");
		} catch (OpenViduException e) {
			return;
		}

//		sessionManager.leaveRoom(participant, request.getId(), EndReason.disconnect, true);
		sessionManager.leaveRoom(participant, request.getId(), EndReason.disconnect, false);
		log.info("Participant {} has left session {}", participant.getParticipantPublicId(),
				rpcConnection.getSessionId());
	}

	private void publishVideo(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "publish");
		} catch (OpenViduException e) {
			return;
		}

		if (sessionManager.isPublisherInSession(rpcConnection.getSessionId(), participant)) {
			MediaOptions options = sessionManager.generateMediaOptions(request);
			sessionManager.publishVideo(participant, options, request.getId());
		} else {
			log.error("Error: participant {} is not a publisher", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to publish video. The user does not have a valid token");
		}
	}

	private void receiveVideoFrom(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "subscribe");
		} catch (OpenViduException e) {
			return;
		}

		String senderName = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SENDER_PARAM);
		senderName = senderName.substring(0, senderName.indexOf("_"));
		String sdpOffer = getStringParam(request, ProtocolElements.RECEIVEVIDEO_SDPOFFER_PARAM);

		sessionManager.subscribe(participant, senderName, sdpOffer, request.getId());
	}

	private void unsubscribeFromVideo(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "unsubscribe");
		} catch (OpenViduException e) {
			return;
		}

		String senderName = getStringParam(request, ProtocolElements.UNSUBSCRIBEFROMVIDEO_SENDER_PARAM);
		sessionManager.unsubscribe(participant, senderName, request.getId());
	}

	private void onIceCandidate(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "onIceCandidate");
		} catch (OpenViduException e) {
			return;
		}

		String endpointName = getStringParam(request, ProtocolElements.ONICECANDIDATE_EPNAME_PARAM);
		String candidate = getStringParam(request, ProtocolElements.ONICECANDIDATE_CANDIDATE_PARAM);
		String sdpMid = getStringParam(request, ProtocolElements.ONICECANDIDATE_SDPMIDPARAM);
		int sdpMLineIndex = getIntParam(request, ProtocolElements.ONICECANDIDATE_SDPMLINEINDEX_PARAM);

		sessionManager.onIceCandidate(participant, endpointName, candidate, sdpMLineIndex, sdpMid, request.getId());
	}

	private void sendMessage(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "signal");
		} catch (OpenViduException e) {
			return;
		}

		String message = getStringParam(request, ProtocolElements.SENDMESSAGE_MESSAGE_PARAM);
		sessionManager.sendMessage(participant, message, request.getId());
	}

	private void unpublishVideo(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "unpublish");
		} catch (OpenViduException e) {
			return;
		}

		sessionManager.unpublishVideo(participant, null, request.getId(), EndReason.unpublish);
	}

	private void streamPropertyChanged(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "onStreamPropertyChanged");
		} catch (OpenViduException e) {
			return;
		}

		String streamId = getStringParam(request, ProtocolElements.STREAMPROPERTYCHANGED_STREAMID_PARAM);
		String property = getStringParam(request, ProtocolElements.STREAMPROPERTYCHANGED_PROPERTY_PARAM);
		JsonElement newValue = getParam(request, ProtocolElements.STREAMPROPERTYCHANGED_NEWVALUE_PARAM);
		String reason = getStringParam(request, ProtocolElements.STREAMPROPERTYCHANGED_REASON_PARAM);

		sessionManager.streamPropertyChanged(participant, request.getId(), streamId, property, newValue, reason);
	}

	private void forceDisconnect(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "forceDisconnect");
		} catch (OpenViduException e) {
			return;
		}

		if (sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant)) {
			String connectionId = getStringParam(request, ProtocolElements.FORCEDISCONNECT_CONNECTIONID_PARAM);
			sessionManager.evictParticipant(
					sessionManager.getSession(rpcConnection.getSessionId()).getParticipantByPublicId(connectionId),
					participant, request.getId(), EndReason.forceDisconnectByUser);
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to force disconnect. The user does not have a valid token");
		}
	}

	private void forceUnpublish(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "forceUnpublish");
		} catch (OpenViduException e) {
			return;
		}

		if (sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant)) {
			String streamId = getStringParam(request, ProtocolElements.FORCEUNPUBLISH_STREAMID_PARAM);
			sessionManager.unpublishStream(sessionManager.getSession(rpcConnection.getSessionId()), streamId,
					participant, request.getId(), EndReason.forceUnpublishByUser);
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to force unpublish. The user does not have a valid token");
		}
	}

	private void applyFilter(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "applyFilter");
		} catch (OpenViduException e) {
			return;
		}

		String filterType = getStringParam(request, ProtocolElements.FILTER_TYPE_PARAM);
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow applying filter if the user is a MODERATOR (owning the stream or other
		// user's stream) or if the user is the owner of the stream and has a token
		// configured with this specific filter
		if (isModerator || (this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId))) {
//				&& participant.getToken().getKurentoTokenOptions().isFilterAllowed(filterType))) {
			JsonObject filterOptions;
			try {
				filterOptions = new JsonParser().parse(getStringParam(request, ProtocolElements.FILTER_OPTIONS_PARAM))
						.getAsJsonObject();
			} catch (JsonSyntaxException e) {
				throw new OpenViduException(Code.FILTER_NOT_APPLIED_ERROR_CODE,
						"'options' parameter is not a JSON object: " + e.getMessage());
			}
			Participant moderator = isModerator ? participant : null;
			sessionManager.applyFilter(sessionManager.getSession(rpcConnection.getSessionId()), streamId, filterType,
					filterOptions, moderator, request.getId(), "applyFilter");
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to apply filter. The user does not have a valid token");
		}
	}

	private void removeFilter(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "removeFilter");
		} catch (OpenViduException e) {
			return;
		}
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow removing filter if the user is a MODERATOR (owning the stream or other
		// user's stream) or if the user is the owner of the stream
		if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
			Participant moderator = isModerator ? participant : null;
			sessionManager.removeFilter(sessionManager.getSession(rpcConnection.getSessionId()), streamId, moderator,
					request.getId(), "removeFilter");
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to remove filter. The user does not have a valid token");
		}
	}

	private void execFilterMethod(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "execFilterMethod");
		} catch (OpenViduException e) {
			return;
		}
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		String filterMethod = getStringParam(request, ProtocolElements.FILTER_METHOD_PARAM);
		JsonObject filterParams = new JsonParser().parse(getStringParam(request, ProtocolElements.FILTER_PARAMS_PARAM))
				.getAsJsonObject();
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow executing filter method if the user is a MODERATOR (owning the stream
		// or other user's stream) or if the user is the owner of the stream
		if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
			Participant moderator = isModerator ? participant : null;
			sessionManager.execFilterMethod(sessionManager.getSession(rpcConnection.getSessionId()), streamId,
					filterMethod, filterParams, moderator, request.getId(), "execFilterMethod");
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to execute filter method. The user does not have a valid token");
		}
	}

	private void addFilterEventListener(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "addFilterEventListener");
		} catch (OpenViduException e) {
			return;
		}
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		String eventType = getStringParam(request, ProtocolElements.FILTEREVENTLISTENER_EVENTTYPE_PARAM);
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow adding a filter event listener if the user is a MODERATOR (owning the
		// stream or other user's stream) or if the user is the owner of the stream
		if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
			try {
				sessionManager.addFilterEventListener(sessionManager.getSession(rpcConnection.getSessionId()),
						participant, streamId, eventType);
				this.notificationService.sendResponse(participant.getParticipantPrivateId(), request.getId(),
						new JsonObject());
			} catch (OpenViduException e) {
				this.notificationService.sendErrorResponse(participant.getParticipantPrivateId(), request.getId(),
						new JsonObject(), e);
			}
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to add filter event listener. The user does not have a valid token");
		}
	}

	private void removeFilterEventListener(RpcConnection rpcConnection, Request<JsonObject> request) {
		Participant participant;
		try {
			participant = sanityCheckOfSession(rpcConnection, "removeFilterEventListener");
		} catch (OpenViduException e) {
			return;
		}
		String streamId = getStringParam(request, ProtocolElements.FILTER_STREAMID_PARAM);
		String eventType = getStringParam(request, ProtocolElements.FILTEREVENTLISTENER_EVENTTYPE_PARAM);
		boolean isModerator = this.sessionManager.isModeratorInSession(rpcConnection.getSessionId(), participant);

		// Allow removing a filter event listener if the user is a MODERATOR (owning the
		// stream or other user's stream) or if the user is the owner of the stream
		if (isModerator || this.userIsStreamOwner(rpcConnection.getSessionId(), participant, streamId)) {
			try {
				sessionManager.removeFilterEventListener(sessionManager.getSession(rpcConnection.getSessionId()),
						participant, streamId, eventType);
				this.notificationService.sendResponse(participant.getParticipantPrivateId(), request.getId(),
						new JsonObject());
			} catch (OpenViduException e) {
				this.notificationService.sendErrorResponse(participant.getParticipantPrivateId(), request.getId(),
						new JsonObject(), e);
			}
		} else {
			log.error("Error: participant {} is not a moderator", participant.getParticipantPublicId());
			throw new OpenViduException(Code.USER_UNAUTHORIZED_ERROR_CODE,
					"Unable to remove filter event listener. The user does not have a valid token");
		}
	}

	public void leaveRoomAfterConnClosed(String participantPrivateId, EndReason reason) {
		try {
			sessionManager.evictParticipant(this.sessionManager.getParticipant(participantPrivateId), null, null,
					reason);
			log.info("Evicted participant with privateId {}", participantPrivateId);
		} catch (OpenViduException e) {
			log.warn("Unable to evict: {}", e.getMessage());
			log.trace("Unable to evict user", e);
		}
	}

	@Override
	public void afterConnectionEstablished(Session rpcSession) throws Exception {
		log.info("After connection established for WebSocket session: {}", rpcSession.getSessionId());
		if (rpcSession instanceof WebSocketServerSession) {
			InetAddress address;
			HttpHeaders headers = ((WebSocketServerSession) rpcSession).getWebSocketSession().getHandshakeHeaders();
			if (headers.containsKey("x-real-ip")) {
				address = InetAddress.getByName(headers.get("x-real-ip").get(0));
			} else {
				address = ((WebSocketServerSession) rpcSession).getWebSocketSession().getRemoteAddress().getAddress();
			}
			rpcSession.getAttributes().put("remoteAddress", address);

			HttpSession httpSession = (HttpSession) ((WebSocketServerSession) rpcSession).getWebSocketSession()
					.getAttributes().get("httpSession");
			rpcSession.getAttributes().put("httpSession", httpSession);
		}
	}

	@Override
	public void afterConnectionClosed(Session rpcSession, String status) throws Exception {
		log.info("After connection closed for WebSocket session: {} - Status: {}", rpcSession.getSessionId(), status);

		String rpcSessionId = rpcSession.getSessionId();
		String message = "";

		if ("Close for not receive ping from client".equals(status)) {
			message = "Evicting participant with private id {} because of a network disconnection";
		} else if (status == null) { // && this.webSocketBrokenPipeTransportError.remove(rpcSessionId) != null)) {
			try {
				Participant p = sessionManager.getParticipant(rpcSession.getSessionId());
				if (p != null) {
					message = "Evicting participant with private id {} because its websocket unexpectedly closed in the client side";
				}
			} catch (OpenViduException exception) {
			}
		}

		if (!message.isEmpty()) {
			RpcConnection rpc = this.notificationService.closeRpcSession(rpcSessionId);
			if (rpc != null && rpc.getSessionId() != null) {
				io.openvidu.server.core.Session session = this.sessionManager.getSession(rpc.getSessionId());
				if (session != null && session.getParticipantByPrivateId(rpc.getParticipantPrivateId()) != null) {
					log.info(message, rpc.getParticipantPrivateId());
					leaveRoomAfterConnClosed(rpc.getParticipantPrivateId(), EndReason.networkDisconnect);
				}
			}
		}

		if (this.webSocketEOFTransportError.remove(rpcSessionId) != null) {
			log.warn(
					"Evicting participant with private id {} because a transport error took place and its web socket connection is now closed",
					rpcSession.getSessionId());
			this.leaveRoomAfterConnClosed(rpcSessionId, EndReason.networkDisconnect);
		}
	}

	@Override
	public void handleTransportError(Session rpcSession, Throwable exception) throws Exception {
		log.error("Transport exception for WebSocket session: {} - Exception: {}", rpcSession.getSessionId(),
				exception.getMessage());
		if ("IOException".equals(exception.getClass().getSimpleName())
				&& "Broken pipe".equals(exception.getCause().getMessage())) {
			log.warn("Parcipant with private id {} unexpectedly closed the websocket", rpcSession.getSessionId());
		}
		if ("EOFException".equals(exception.getClass().getSimpleName())) {
			// Store WebSocket connection interrupted exception for this web socket to
			// automatically evict the participant on "afterConnectionClosed" event
			this.webSocketEOFTransportError.put(rpcSession.getSessionId(), true);
		}
	}

	@Override
	public void handleUncaughtException(Session rpcSession, Exception exception) {
		log.error("Uncaught exception for WebSocket session: {} - Exception: {}", rpcSession.getSessionId(), exception);
	}

	@Override
	public List<String> allowedOrigins() {
		return Arrays.asList("*");
	}

	public static String getStringParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key).getAsString();
	}

	public static int getIntParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key).getAsInt();
	}

	public static boolean getBooleanParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key).getAsBoolean();
	}

	public static JsonElement getParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key);
	}

	private Participant sanityCheckOfSession(RpcConnection rpcConnection, String methodName) throws OpenViduException {
		String participantPrivateId = rpcConnection.getParticipantPrivateId();
		String sessionId = rpcConnection.getSessionId();
		String errorMsg;

		if (sessionId == null) { // null when afterConnectionClosed
			errorMsg = "No session information found for participant with privateId " + participantPrivateId
					+ ". Using the admin method to evict the user.";
			log.warn(errorMsg);
			leaveRoomAfterConnClosed(participantPrivateId, null);
			throw new OpenViduException(Code.GENERIC_ERROR_CODE, errorMsg);
		} else {
			// Sanity check: don't call RPC method unless the id checks out
			Participant participant = sessionManager.getParticipant(sessionId, participantPrivateId);
			if (participant != null) {
				errorMsg = "Participant " + participant.getParticipantPublicId() + " is calling method '" + methodName
						+ "' in session " + sessionId;
				log.info(errorMsg);
				return participant;
			} else {
				errorMsg = "Participant with private id " + participantPrivateId + " not found in session " + sessionId
						+ ". Using the admin method to evict the user.";
				log.warn(errorMsg);
				leaveRoomAfterConnClosed(participantPrivateId, null);
				throw new OpenViduException(Code.GENERIC_ERROR_CODE, errorMsg);
			}
		}
	}

	private boolean userIsStreamOwner(String sessionId, Participant participant, String streamId) {
		return participant.getParticipantPrivateId()
				.equals(this.sessionManager.getParticipantPrivateIdFromStreamId(sessionId, streamId));
	}

}
