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

import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.OpenViduException.Code;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.AuthorizationManage;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.kurento.core.KurentoParticipant;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RpcHandler extends DefaultJsonRpcHandler<JsonObject> {

	private static final Logger log = LoggerFactory.getLogger(RpcHandler.class);

    @Resource
	RpcHandlerFactory rpcHandlerFactory;

	@Autowired
	SessionManager sessionManager;

	@Autowired
	RpcNotificationService notificationService;

	@Autowired
	CacheManage cacheManage;

    @Autowired
    AuthorizationManage authorizationManage;

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
		if (Objects.isNull(rpcAbstractHandler)) {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.UNRECOGNIZED_API);
			return;
		}
		rpcAbstractHandler.handRpcRequest(rpcConnection, request);
	}


	@Override
	public void afterConnectionEstablished(Session rpcSession) {
		log.info("After connection established for WebSocket session: {}", rpcSession.getSessionId());
	}

	@Override
	public void afterConnectionClosed(Session rpcSession, String status) throws Exception {
		log.info("After connection closed for WebSocket session: {} - Status: {}", rpcSession.getSessionId(), status);
		String rpcSessionId = rpcSession.getSessionId();
		String message = "";
		Participant p = null;
		RpcConnection rpcConnection;

		// update user online status in cache
		if ((rpcConnection = notificationService.getRpcConnection(rpcSessionId)) != null) {
			if (Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web)) {
				sessionManager.accessOut(rpcConnection);
				return;
			}
			cacheManage.updateUserOnlineStatus(notificationService.getRpcConnection(rpcSessionId).getUserUuid(),
					UserOnlineStatusEnum.offline);
			if (!Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.web) && !Objects.isNull(rpcConnection.getSerialNumber())) {
				cacheManage.setDeviceStatus(rpcConnection.getSerialNumber(), DeviceStatus.offline.name());
			}
		} else {
			log.info("=====>can not find this rpc connection:{} in notificationService maps.", rpcSessionId);
		}
		if ("Close for not receive ping from client".equals(status)) {
			message = "Evicting participant with private id {} because of a network disconnection";
		} else if ("Connection reset by peer".equals(status)) {
			message = "Evicting participant with private id {} because of connection reset by peer";
		} else if (status == null) { // && this.webSocketBrokenPipeTransportError.remove(rpcSessionId) != null)) {
			try {
				p = sessionManager.getParticipant(rpcSession.getSessionId());
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

					// release audio composite
					KurentoParticipant kp = (KurentoParticipant) participant;
					if (!Objects.isNull(kp.getPublisher()))
						kp.getPublisher().closeAudioComposite();

					notifyUserBreakLine(session.getSessionId(), participant.getParticipantPublicId());
					// send end roll notify if the offline connection's hand status is speaker
					p = !Objects.isNull(p) ? p : this.sessionManager.getParticipant(rpcSessionId);
					if (!Objects.isNull(p) && Objects.equals(ParticipantHandStatus.speaker, p.getHandStatus())) {
						p.setHandStatus(ParticipantHandStatus.endSpeaker);

						JsonObject params = new JsonObject();
						params.addProperty(ProtocolElements.END_ROLL_CALL_ROOM_ID_PARAM, p.getSessionId());
						params.addProperty(ProtocolElements.END_ROLL_CALL_TARGET_ID_PARAM, rpc.getUserId());
						this.sessionManager.getParticipants(p.getSessionId()).forEach(part -> {
							if (!Objects.equals(rpcSessionId, part.getParticipantPrivateId()))
								this.notificationService.sendNotification(part.getParticipantPrivateId(),
										ProtocolElements.END_ROLL_CALL_METHOD, params);
						});
					}
				}
			}
		}

		if (this.webSocketEOFTransportError.remove(rpcSessionId) != null) {
			log.warn(
					"Evicting participant with private id {} because a transport error took place and its web socket connection is now closed",
					rpcSession.getSessionId());
		}
	}

	@Override
	public void handleTransportError(Session rpcSession, Throwable exception) throws Exception {
		// update user online status in cache
		if (rpcSession != null) {
			if (notificationService.getRpcConnection(rpcSession.getSessionId()) != null &&
					Objects.equals(AccessTypeEnum.terminal, notificationService.getRpcConnection(rpcSession.getSessionId()).getAccessType()))
				cacheManage.updateUserOnlineStatus(notificationService.getRpcConnection(rpcSession.getSessionId()).getUserUuid(),
						UserOnlineStatusEnum.offline);
			log.error("Transport exception for WebSocket session: {} - Exception: {}", rpcSession.getSessionId(),
					exception.getMessage());
			if ("IOException".equals(exception.getClass().getSimpleName())) {
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
				if (Objects.equals(cacheManage.getUserInfoByUUID(rpc.getUserUuid()).get("status"), UserOnlineStatusEnum.online.name())
						|| Objects.equals(OpenViduRole.THOR, p.getRole())) {
					notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.USER_BREAK_LINE_METHOD, params);
				}
			}
		});
	}

	public void cleanSession(String sessionId, EndReason reason) {
		if (Objects.isNull(sessionManager.getSession(sessionId)) || sessionManager.getSession(sessionId).isClosed()) {
			return;
		}
		// 1. notify all participant stop publish and receive stream.
		// 2. close session but can not disconnect the connection.
		sessionManager.getSession(sessionId).getParticipants().forEach(p ->
				notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject()));
		this.sessionManager.unpublishAllStream(sessionId, reason);
		this.sessionManager.closeSession(sessionId, reason);
	}

	public RpcNotificationService getNotificationService() {
		return notificationService;
	}
}
