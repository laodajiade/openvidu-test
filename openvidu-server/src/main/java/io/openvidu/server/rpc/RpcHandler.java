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

import com.google.gson.*;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.*;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.AuthorizationManage;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.manage.DeviceManage;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.*;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.utils.GeoLocation;
import io.openvidu.server.utils.GeoLocationByIp;
import io.openvidu.server.utils.StringUtil;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class RpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory.getLogger(RpcHandler.class);

	private static final Gson gson = new GsonBuilder().create();

	@Resource
	RpcHandlerFactory rpcHandlerFactory;

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
    ConferenceMapper conferenceMapper;

	@Resource
	DeviceMapper deviceMapper;

	@Resource
	DeviceDeptMapper deviceDeptMapper;

	@Resource
	UserDeptMapper userDeptMapper;

	@Resource
	DepartmentMapper depMapper;

	@Autowired
    AuthorizationManage authorizationManage;

	@Autowired
	DepartmentManage departmentManage;

	@Autowired
    DeviceManage deviceManage;

	@Autowired
    UserManage userManage;

	private ConcurrentMap<String, Boolean> webSocketEOFTransportError = new ConcurrentHashMap<>();

	@Override
	public void handleRequest(Transaction transaction, Request<JsonObject> request) throws Exception {

		String participantPrivateId;
		try {
			participantPrivateId = transaction.getSession().getSessionId();
		} catch (Throwable e) {
			log.error("Error getting WebSocket session ID from transaction {}", transaction, e);
			throw e;
		}

		log.info("WebSocket request session #{} - Request: {}", participantPrivateId, request);

		RpcConnection rpcConnection = null;
		if (ProtocolElements.ACCESS_IN_METHOD.equals(request.getMethod())) {
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

		// Authorization Check
//        if (authorizationManage.checkIfOperationPermitted(request.getMethod(), rpcConnection)) {
//            assert rpcConnection != null;
//            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
//                    null, ErrorCodeEnum.PERMISSION_LIMITED);
//            return;
//        }

		rpcConnection = notificationService.addTransaction(transaction, request);
		request.setSessionId(rpcConnection.getParticipantPrivateId());

		String sessionId = rpcConnection.getSessionId();
		if (sessionId == null && !ProtocolElements.FILTERS.contains(request.getMethod())) {
			log.warn(
					"No session information found for participant with privateId {} when trying to execute method '{}'. Method 'Session.connect()' must be the first operation called in any session",
					participantPrivateId, request.getMethod());
			throw new OpenViduException(Code.TRANSPORT_ERROR_CODE,
					"No session information found for participant with privateId " + participantPrivateId
							+ ". Method 'Session.connect()' must be the first operation called in any session");
		}

		transaction.startAsync();
		RpcAbstractHandler rpcAbstractHandler = rpcHandlerFactory.getRpcHandler(request.getMethod());
		rpcAbstractHandler.handRpcRequest(rpcConnection, request);
	}


	private boolean isExistingRoom(String sessionId, String userUuid) {
		// verify room id ever exists
		ConferenceSearch search = new ConferenceSearch();
		search.setRoomId(sessionId);
		// 会议状态：0 未开始(当前不存在该状态) 1 进行中 2 已结束
		search.setStatus(1);
		try {
			List<Conference> conferences = conferenceMapper.selectBySearchCondition(search);
			if (conferences != null && !conferences.isEmpty()) {
				if (sessionId.equals(userUuid)) {
					// force close previous room when sessionId is userUuid.
					log.warn("conference:{} will be force closed.", sessionId);
					// TODO
					conferences.forEach(conference -> sessionManager.endConferenceInfo(conference));
					cleanSession(sessionId, "", false, EndReason.forceCloseSessionByUser);
					return false;
				}

				log.warn("conference:{} already exist.", sessionId);
				return true;
			}
		} catch (Exception e) {
			log.info("conferenceMapper selectBySearchCondition(search) exception {}", e);
		}
		return false;
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
	public void afterConnectionEstablished(Session rpcSession) {
		log.info("After connection established for WebSocket session: {}", rpcSession.getSessionId());
		/*if (rpcSession instanceof WebSocketServerSession) {
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
		}*/
	}

	@Override
	public void afterConnectionClosed(Session rpcSession, String status) throws Exception {
		log.info("After connection closed for WebSocket session: {} - Status: {}", rpcSession.getSessionId(), status);
		String rpcSessionId = rpcSession.getSessionId();
		String message = "";

		// update user online status in cache
        if (notificationService.getRpcConnection(rpcSessionId) != null)
		    cacheManage.updateUserOnlineStatus(notificationService.getRpcConnection(rpcSessionId).getUserUuid(),
				UserOnlineStatusEnum.offline);
		if ("Close for not receive ping from client".equals(status)) {
			message = "Evicting participant with private id {} because of a network disconnection";
		} else if (status == null) { // && this.webSocketBrokenPipeTransportError.remove(rpcSessionId) != null)) {
			try {
				Participant p = sessionManager.getParticipant(rpcSession.getSessionId());
				if (p != null) {
					message = "Evicting participant with private id {} because its websocket unexpectedly closed in the client side";
				}
			} catch (OpenViduException ex) {
				log.error("Exception:\n", ex);
			}
		}

		if (!message.isEmpty()) {
//			RpcConnection rpc = this.notificationService.closeRpcSession(rpcSessionId);
			RpcConnection rpc = this.notificationService.getRpcConnection(rpcSessionId);
			if (rpc != null && rpc.getSessionId() != null) {
				io.openvidu.server.core.Session session = this.sessionManager.getSession(rpc.getSessionId());
				Participant participant;
				if (session != null && (participant = session.getParticipantByPrivateId(rpc.getParticipantPrivateId())) != null) {
					log.info(message, rpc.getParticipantPrivateId());
//					leaveRoomAfterConnClosed(rpc.getParticipantPrivateId(), EndReason.networkDisconnect);
//					cacheManage.updateUserOnlineStatus(rpc.getUserUuid(), UserOnlineStatusEnum.offline);

					notifyUserBreakLine(session.getSessionId(), participant.getParticipantPublicId());
				}
			}
		}

		if (this.webSocketEOFTransportError.remove(rpcSessionId) != null) {
			log.warn(
					"Evicting participant with private id {} because a transport error took place and its web socket connection is now closed",
					rpcSession.getSessionId());
//			this.leaveRoomAfterConnClosed(rpcSessionId, EndReason.networkDisconnect);
			/*try {
				Participant p = this.sessionManager.getParticipant(rpcSessionId);
				RpcConnection c = this.notificationService.getRpcConnection(rpcSessionId);
				notifyUserBreakLine(p.getSessionId(), c.getUserId());
			} catch(OpenViduException e) {
				log.info("exception:{}", e);
			}*/
		}
	}

	@Override
	public void handleTransportError(Session rpcSession, Throwable exception) throws Exception {
		// update user online status in cache
		if (rpcSession != null) {
			if (notificationService.getRpcConnection(rpcSession.getSessionId()) != null)
				cacheManage.updateUserOnlineStatus(notificationService.getRpcConnection(rpcSession.getSessionId()).getUserUuid(),
						UserOnlineStatusEnum.offline);
			log.error("Transport exception for WebSocket session: {} - Exception: {}", rpcSession.getSessionId(),
					exception.getMessage());
			if ("IOException".equals(exception.getClass().getSimpleName())) {
//					&& "Broken pipe".equals(exception.getCause().getMessage())) {
				log.warn("Parcipant with private id {} unexpectedly closed the websocket", rpcSession.getSessionId());
			}
			if ("EOFException".equals(exception.getClass().getSimpleName())) {
				// Store WebSocket connection interrupted exception for this web socket to
				// automatically evict the participant on "afterConnectionClosed" event
				this.webSocketEOFTransportError.put(rpcSession.getSessionId(), true);
			}
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

	public static List<String> getStringListParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null || !request.getParams().get(key).isJsonArray()) {
			return null;
		}

		List<String> values = new ArrayList<>();
		request.getParams().get(key).getAsJsonArray().forEach(s -> values.add(s.getAsString()));
		return values;
	}

	public static String getStringParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key).getAsString();
	}

	public static String getStringOptionalParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			return null;
		}

		return request.getParams().get(key).getAsString();
	}

	public static int getIntParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("RMBER");
		}
		return request.getParams().get(key).getAsInt();
	}

	public static Integer getIntOptionalParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			return null;
		}

		return request.getParams().get(key).getAsInt();
	}

	public static Float getFloatOptionalParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			return null;
		}

		return request.getParams().get(key).getAsFloat();
	}

	public static boolean getBooleanParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key).getAsBoolean();
	}

	public static long getLongParam(Request<JsonObject> request, String key) {
		if (request.getParams() == null || request.getParams().get(key) == null) {
			throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
					+ "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
		}
		return request.getParams().get(key).getAsLong();
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

	private Participant sanityCheckOfSession(RpcConnection rpcConnection, StreamType streamType) throws OpenViduException {
		Participant participant = sessionManager.getParticipant(rpcConnection.getSessionId(),
				rpcConnection.getParticipantPrivateId(), streamType);
		if (participant == null) {
			leaveRoomAfterConnClosed(rpcConnection.getParticipantPrivateId(), null);
			throw new OpenViduException(Code.GENERIC_ERROR_CODE, "Participant not exists.");
		}
		return participant;
	}

	private Participant sanityCheckOfSession(RpcConnection rpcConnection, String participantPublicId, String methodName) throws OpenViduException {
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
			Participant participant = sessionManager.getParticipantByPrivateAndPublicId(sessionId, participantPrivateId, participantPublicId);
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

	public ErrorCodeEnum cleanSession(String sessionId, String privateId, boolean checkModerator, EndReason reason) {
		if (Objects.isNull(sessionManager.getSession(sessionId))) {
			return ErrorCodeEnum.CONFERENCE_NOT_EXIST;
		}

		if (sessionManager.getSession(sessionId).isClosed()) {
			return ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED;
		}

		if (checkModerator && sessionManager.getParticipant(sessionId, privateId).getRole() != OpenViduRole.MODERATOR) {
			return ErrorCodeEnum.PERMISSION_LIMITED;
		}

		// 1. notify all participant stop publish and receive stream.
		// 2. close session but can not disconnect the connection.
		this.sessionManager.unpublishAllStream(sessionId, reason);
		this.sessionManager.closeSession(sessionId, reason);

		return ErrorCodeEnum.SUCCESS;
	}


	private boolean isModerator(String role) {
		// TODO. Fixme. user account have moderator power.
		if (Objects.equals(OpenViduRole.MODERATOR.name(), role)) {
//		if (OpenViduRole.MODERATOR.equals(OpenViduRole.valueOf(role))) {
			return true;
		}
		return false;
	}

	public SessionManager getSessionManager() { return this.sessionManager; }

	public void notifyRoomCountdown(String sessionId, long remainTime) {
		JsonObject params = new JsonObject();

		params.addProperty(ProtocolElements.ROOM_COUNTDOWN_INFO_ID_PARAM, sessionId);
		params.addProperty(ProtocolElements.ROOM_COUNTDOWN_TIME_PARAM, remainTime);
		sessionManager.getParticipants(sessionId).forEach(p ->
				notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.ROOM_COUNTDOWN_METHOD, params));
	}


	private void notifyUserBreakLine(String sessionId, String publicId) {
		JsonObject params = new JsonObject();
		params.addProperty(ProtocolElements.USER_BREAK_LINE_CONNECTION_ID_PARAM, publicId);

		sessionManager.getParticipants(sessionId).forEach(p -> {
			RpcConnection rpc = notificationService.getRpcConnection(p.getParticipantPrivateId());
			if (rpc != null) {
				if (Objects.equals(cacheManage.getUserInfoByUUID(rpc.getUserUuid()).get("status"), UserOnlineStatusEnum.online.name())) {
					notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.USER_BREAK_LINE_METHOD, params);
				}
			}
		});
	}

	private boolean updateReconnectInfo(RpcConnection rpcConnection) {
		try {
			Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
			if (Objects.isNull(userInfo)) {
				log.warn("user:{} info is null.", rpcConnection.getUserUuid());
				return false;
			}

			if (Objects.equals(UserOnlineStatusEnum.reconnect.name(), userInfo.get("status"))) {
				log.info("reconnect userId:{} mac:{}", rpcConnection.getUserId(), rpcConnection.getMacAddr());
				String oldPrivateId = String.valueOf(userInfo.get("reconnect"));
				if (StringUtils.isEmpty(oldPrivateId)) {
					log.warn("reconnect privateId:{}", oldPrivateId);
					return false;
				}

				RpcConnection oldRpcConnection = notificationService.getRpcConnection(oldPrivateId);
				cacheManage.updateUserOnlineStatus(rpcConnection.getUserUuid(), UserOnlineStatusEnum.online);
				cacheManage.updateReconnectInfo(rpcConnection.getUserUuid(), "");
				leaveRoomAfterConnClosed(oldPrivateId, EndReason.sessionClosedByServer);
//				accessOut(oldRpcConnection, null);
				sessionManager.accessOut(oldRpcConnection);
				return true;
			}
		} catch (Exception e) {
			log.warn("exception:{}", e);
			return false;
		}

		return true;
	}

	public RpcNotificationService getNotificationService() {
		return notificationService;
	}
}
