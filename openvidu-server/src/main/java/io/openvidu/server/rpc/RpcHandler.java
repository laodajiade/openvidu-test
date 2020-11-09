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
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.common.manage.AuthorizationManage;
import io.openvidu.server.core.SessionManager;
import org.kurento.jsonrpc.DefaultJsonRpcHandler;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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


	@Override
	public void handleRequest(Transaction transaction, Request<JsonObject> request) {

		String participantPrivateId;
		try {
			participantPrivateId = transaction.getSession().getSessionId();
		} catch (Throwable e) {
			log.error("Error getting WebSocket session ID from transaction {}", transaction, e);
			throw e;
		}

		log.info("WebSocket request session #{} - Request: {}", participantPrivateId, request);

		if (ProtocolElements.ACCESS_IN_METHOD.equals(request.getMethod())) {
			notificationService.newRpcConnection(transaction, request);
		} else if (notificationService.getRpcConnection(participantPrivateId) == null) {
			// return errorCode:11007("please access in first") if any method is called before 'accessIn'
			log.warn(
					"No connection found for participant with privateId {} when trying to execute method '{}'. " +
							"Method 'Session.connect()' must be the first operation called in any session", participantPrivateId, request.getMethod());
			notificationService.sendRespWithConnTransaction(transaction, request.getId(), ErrorCodeEnum.ACCESS_IN_NEEDED);
			return;
		}

		RpcConnection rpcConnection = notificationService.addTransaction(transaction, request);
		request.setSessionId(rpcConnection.getParticipantPrivateId());

		RpcAbstractHandler rpcAbstractHandler = rpcHandlerFactory.getRpcHandler(request.getMethod());
		if (Objects.isNull(rpcAbstractHandler)) {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.UNRECOGNIZED_API);
			return;
		}

		String sessionId = rpcConnection.getSessionId();
		if (sessionId == null && !ProtocolElements.FILTERS.contains(request.getMethod())) {
			log.warn(
					"No session information found for participant with privateId {} when trying to execute method '{}'. " +
							"Method 'joinRoom' must be the first operation called in this case.", participantPrivateId, request.getMethod());
			notificationService.sendErrorResponseWithDesc(participantPrivateId, request.getId(), null, ErrorCodeEnum.JOIN_ROOM_NEEDED);
			return;
		}

		if (ProtocolElements.CORP_SERVICE_EXPIRED_FILTERS.contains(request.getMethod()) && cacheManage.getCorpExpired(rpcConnection.getProject())) {
			notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
					null, ErrorCodeEnum.CORP_SERVICE_EXPIRED);
			return;
		}

		transaction.startAsync();
		rpcAbstractHandler.handRpcRequest(rpcConnection, request);
	}


	@Override
	public void afterConnectionEstablished(Session rpcSession) {
		log.info("After connection established for WebSocket session: {}", rpcSession.getSessionId());
	}

	@Override
	public void afterConnectionClosed(Session rpcSession, String status) {
		RpcConnection rpc;
		if (Objects.isNull(rpcSession) ||
				Objects.isNull(rpc = this.notificationService.getRpcConnection(rpcSession.getSessionId()))) {
			log.info("The connection already cleaned up when event 'afterConnectionClosed' callback.");
			return;
		}

		boolean overKeepAlive = false;
		log.info("After connection closed for WebSocket session: {} - Status: {}", rpcSession.getSessionId(), status);
		if (!Objects.isNull(status)) {
			String message;
			switch (status) {
				case "Close for not receive ping from client":
					overKeepAlive = true;
					message = "Evicting participant with private id {} because of a network disconnection";
					break;
				case "Connection reset by peer":
					message = "Evicting participant with private id {} because of connection reset by peer";
					break;
				default:
					message = "Evicting participant with private id {} because its websocket unexpectedly closed in the client side";
					break;
			}
			log.error(message, rpc.getParticipantPrivateId());
		} else {
			log.info("afterConnectionClosed and the status is null, private id : {}", rpcSession.getSessionId());
		}

		// change the terminal status if the ws link accessIn succeeded
        cacheManage.updateTerminalStatus(rpc, TerminalStatus.offline);
        if (AccessTypeEnum.terminal.equals(rpc.getAccessType()) && Objects.nonNull(rpc.getTerminalType())
                && Objects.nonNull(rpc.getUserUuid()) && Objects.nonNull(rpc.getSessionId())) {
            // record ws exception link that in room before
            cacheManage.recordWsExceptionLink(rpc, overKeepAlive);
        } else {
            // clear the exception ws link directly
            notificationService.closeRpcSession(rpcSession.getSessionId());
        }
	}

	@Override
	public void handleTransportError(Session rpcSession, Throwable exception) throws Exception {
		if (rpcSession != null) {
			log.error("Transport exception for WebSocket session: {} - Exception: {}", rpcSession.getSessionId(),
					exception.getMessage());
		} else {
			log.warn("Transport exception for WebSocket session occurred.");
		}
	}

	@Override
	public void handleUncaughtException(Session rpcSession, Exception exception) {
		if (rpcSession != null) {
			log.error("Uncaught exception for WebSocket session: {} - Exception: {}", rpcSession.getSessionId(),
					exception.getMessage());
		} else {
			log.warn("Uncaught exception for WebSocket session occurred.");
		}
	}

	@Override
	public List<String> allowedOrigins() {
		return Collections.singletonList("*");
	}


	public SessionManager getSessionManager() { return this.sessionManager; }


	public RpcNotificationService getNotificationService() {
		return notificationService;
	}
}
