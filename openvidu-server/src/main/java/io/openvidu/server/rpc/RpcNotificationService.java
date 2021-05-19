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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.client.OpenViduException;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Participant;
import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.kurento.jsonrpc.message.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RpcNotificationService {

	private static final Logger log = LoggerFactory.getLogger(RpcNotificationService.class);

	private ConcurrentMap<String, RpcConnection> rpcConnections = new ConcurrentHashMap<>();

	private static final ThreadPoolExecutor NOTIFY_THREAD_POOL = new ThreadPoolExecutor(
			Math.max(Runtime.getRuntime().availableProcessors() + 1, 4), Math.max(Runtime.getRuntime().availableProcessors() * 20, 20),
			60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
			new ThreadFactoryBuilder().setNameFormat("notify_thread_pool-").setDaemon(true).build());

	public RpcConnection newRpcConnection(Transaction t, Request<JsonObject> request) {
		String participantPrivateId = t.getSession().getSessionId();
		RpcConnection connection = new RpcConnection(t.getSession());
		RpcConnection oldConnection = rpcConnections.putIfAbsent(participantPrivateId, connection);
		if (oldConnection != null) {
			log.warn("Concurrent initialization of rpcSession #{}", participantPrivateId);
			connection = oldConnection;
		}
		return connection;
	}

	public RpcConnection addTransaction(Transaction t, Request<JsonObject> request) {
		String participantPrivateId = t.getSession().getSessionId();
		RpcConnection connection = rpcConnections.get(participantPrivateId);
		connection.addTransaction(request.getId(), t);
		return connection;
	}

	public void sendResponse(String participantPrivateId, Integer transactionId, Object result) {
 		Transaction t = getAndRemoveTransaction(participantPrivateId, transactionId);
		if (t == null) {
			log.error("No transaction {} found for paticipant with private id {}, unable to send result {}",
					transactionId, participantPrivateId, new GsonBuilder().setPrettyPrinting().create().toJson(result));
			return;
		}
		try {
			t.sendResponse(result);
			log.info("WebSocket response session #{} - Response transaction id:{} and result: {}", participantPrivateId, transactionId, result);
		} catch (Exception e) {
			log.error("Exception responding to participant ({})", participantPrivateId, e);
		}
	}

	public void sendErrorResponse(String participantPrivateId, Integer transactionId, Object data,
			OpenViduException error) {
		Transaction t = getAndRemoveTransaction(participantPrivateId, transactionId);
		if (t == null) {
			log.error("No transaction {} found for paticipant with private id {}, unable to send result {}",
					transactionId, participantPrivateId, data);
			return;
		}
		try {
			String dataVal = data != null ? data.toString() : null;
			t.sendError(error.getCodeValue(), error.getMessage(), dataVal);
		} catch (Exception e) {
			log.error("Exception sending error response to user ({})", transactionId, e);
		}
	}

	// add by chosongi 2019-09-12
	public void sendErrorResponseWithDesc(String participantPrivateId, Integer transactionId, Object data,
											  ErrorCodeEnum errorCodeEnum) {
		Transaction t = getAndRemoveTransaction(participantPrivateId, transactionId);
		if (t == null) {
			log.error("No transaction {} found for participant with private id {}, unable to send result {}",
					transactionId, participantPrivateId, data);
			return;
		}
		try {
			String dataVal = data != null ? data.toString() : null;
			t.sendError(errorCodeEnum.getCode(), errorCodeEnum.getMessage(), dataVal);
			log.info("WebSocket error response session #{} - Response transaction id:{} and error:{} {}",
					participantPrivateId, transactionId, errorCodeEnum.getCode(), errorCodeEnum.getMessage());
		} catch (Exception e) {
			log.error("Exception sending error response to user ({})", transactionId, e);
		}
	}

	public void sendNotification(final String participantPrivateId, final String method, final Object params) {
		RpcConnection rpcSession = rpcConnections.get(participantPrivateId);
		if (rpcSession == null || rpcSession.getSession() == null) {
			log.error("No rpc session found for private id {}, unable to send notification {}: {}",
					participantPrivateId, method, params);
			return;
		}
		Session s = rpcSession.getSession();

		try {
			s.sendNotification(method, params);
			log.info("WebSocket notification session #{} - Notification method:{} and params: {}", participantPrivateId, method, params);
		} catch (Exception e) {
			log.error("Exception sending notification '{}': {} to participant with private id {}", method, params,
					participantPrivateId, e);
		}
	}

	public void sendNotificationWithoutLog(final String participantPrivateId, final String method, final Object params) {
		Session s;
		if (Objects.nonNull(s = sanityGetRpcSession(participantPrivateId, method, params))) {
			try {
				s.sendNotification(method, params);
			} catch (IOException e) {
				log.error("Exception sending notification '{}': {} to participant with private id {}", method, params,
						participantPrivateId, e);
			}
		}
	}

	private Session sanityGetRpcSession(final String participantPrivateId, final String method, final Object params) {
		RpcConnection rpcSession = rpcConnections.get(participantPrivateId);
		if (rpcSession == null || rpcSession.getSession() == null) {
			log.error("No rpc session found for private id {}, unable to send notification {}: {}",
					participantPrivateId, method, params);
			return null;
		}
		return rpcSession.getSession();
	}

	public RpcConnection closeRpcSession(String participantPrivateId) {
		RpcConnection rpcSession = rpcConnections.remove(participantPrivateId);
		if (rpcSession == null || rpcSession.getSession() == null) {
			log.error("No session found for private id {}, unable to cleanup", participantPrivateId);
			return null;
		}
		Session s = rpcSession.getSession();
		try {
			s.close();
			log.info("Closed session for participant with private id {}", participantPrivateId);
			this.showRpcConnections();
			return rpcSession;
		} catch (IOException e) {
			log.error("Error closing session for participant with private id {}", participantPrivateId, e);
		}
		return null;
	}

	public void sendBatchNotification(Set<Participant> participants, final String method, final Object params) {
		if (CollectionUtils.isEmpty(participants)) {
			return;
		}
		List<String> participantPrivateIds = participants.stream().map(Participant::getParticipantPrivateId).collect(Collectors.toList());
		sendBatchNotification(participantPrivateIds, method, params);
	}

    public void sendBatchNotification(List<String> participantPrivateIds, final String method, final Object params) {
        List<String> successList = new ArrayList<>();
        List<String> failList = new ArrayList<>();

        for (String participantPrivateId : participantPrivateIds) {
            RpcConnection rpcSession = rpcConnections.get(participantPrivateId);
            if (rpcSession == null || rpcSession.getSession() == null) {
                log.error("No rpc session found for private id {}, unable to send notification {}: {}",
                        participantPrivateId, method, params);
                failList.add(participantPrivateId);
                continue;
            }
            Session s = rpcSession.getSession();
            try {
                s.sendNotification(method, params);
                successList.add(participantPrivateId);
            } catch (Exception e) {
                failList.add(participantPrivateId);
                log.error("Exception sending notification '{}': {} to participant with private id {}", method, params,
                        participantPrivateId, e);
            }
        }
        log.info("batch WebSocket notification- Notification method:{} and params: {}" +
                "successList:{}  failList:{}", method, params, successList, failList);
    }

	public void sendBatchNotificationConcurrent(Set<Participant> participants, final String method, final Object params) {
		List<String> collect = participants.stream().map(Participant::getParticipantPrivateId).collect(Collectors.toList());
		sendBatchNotificationConcurrent(collect, method, params);
	}

    public void sendBatchNotificationUuidConcurrent(Collection<String> uuids, final String method, final Object params) {
        final Set<String> uuidSet = new HashSet<>(uuids);
        List<String> privateIdList = rpcConnections.values().stream().filter(rpcConnection -> uuidSet.contains(rpcConnection.getUserUuid()))
                .map(RpcConnection::getParticipantPrivateId).collect(Collectors.toList());
        sendBatchNotificationConcurrent(privateIdList, method, params);
    }

	/**
	 * sendBatchNotification 的优化版本，使用多线程并发通知，同步接口
	 */
	public void sendBatchNotificationConcurrent(List<String> participantPrivateIds, final String method, final Object params) {
		long start = System.currentTimeMillis();
		List<String> successList = new ArrayList<>();
		List<String> failList = new ArrayList<>();
		int size = participantPrivateIds.size();
		CountDownLatch countDownLatch = new CountDownLatch(size);
		for (String participantPrivateId : participantPrivateIds) {
			NOTIFY_THREAD_POOL.submit(() -> {
				try {
					RpcConnection rpcSession = rpcConnections.get(participantPrivateId);
					if (rpcSession == null || rpcSession.getSession() == null) {
						log.error("No rpc session found for private id {}, unable to send notification {}: {}",
								participantPrivateId, method, params);
						failList.add(participantPrivateId);
						return;
					}
					Session s = rpcSession.getSession();
					try {
						s.sendNotification(method, params);
						successList.add(participantPrivateId);
					} catch (Exception e) {
						failList.add(participantPrivateId);
						log.error("Exception sending notification '{}': {} to participant with private id {}", method, params,
								participantPrivateId, e);
					}
				} finally {
					countDownLatch.countDown();
				}
			});
		}
		try {
			if (!countDownLatch.await((size * 2) + 10, TimeUnit.MILLISECONDS)) {
				log.warn("sendBatchNotificationConcurrent timeout method={},partSize = {}", method, size);
			}
		} catch (InterruptedException e) {
			log.warn("sendBatchNotificationConcurrent error method={},partSize = {}", method, size);
		}
		log.info("sendBatchNotificationConcurrent - Notification method:{} and params: {}" +
				"successList:{}  failList:{}", method, params, successList, failList);
	}

	private Transaction getAndRemoveTransaction(String participantPrivateId, Integer transactionId) {
		RpcConnection rpcSession = rpcConnections.get(participantPrivateId);
		if (rpcSession == null) {
			log.warn("Invalid WebSocket session id {}", participantPrivateId);
			return null;
		}
		log.trace("#{} - {} transactions", participantPrivateId, rpcSession.getTransactions().size());
		Transaction t = rpcSession.getTransaction(transactionId);
		rpcSession.removeTransaction(transactionId);
		return t;
	}

	public void showRpcConnections() {
		log.info("online rpcConnections: {}", this.rpcConnections.size());
	}

	public RpcConnection getRpcConnection(String participantPrivateId) {
		return this.rpcConnections.get(participantPrivateId);
	}

	public Collection<RpcConnection> getRpcConnections() {
		return this.rpcConnections.values();
	}

	public void sendRespWithConnTransaction(Transaction t, Integer requestId, ErrorCodeEnum errorCode) {
		String privateId = null;
		try {
			privateId = t.getSession().getSessionId();
			t.sendError(errorCode.getCode(), errorCode.getMessage(), null);
			log.info("WebSocket error response session #{} - Response transaction id:{} and error:{} {}",
					privateId, requestId, errorCode.getCode(), errorCode.getMessage());

			t.getSession().close();
		} catch (Exception e) {
			log.error("Exception sending error response to privateId ({})", privateId, e);
		}
	}
}
