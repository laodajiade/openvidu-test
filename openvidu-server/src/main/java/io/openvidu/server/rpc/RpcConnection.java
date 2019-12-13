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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.jsonrpc.Session;
import org.kurento.jsonrpc.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object representing client-server WebSocket sessions. Stores information
 * about the connection itself and all the active RPC transactions for each one
 * of them.
 *
 * @author Pablo Fuente (pablofuenteperez@gmail.com)
 */
public class RpcConnection {

	private static final Logger log = LoggerFactory.getLogger(RpcConnection.class);

	private org.kurento.jsonrpc.Session session;
	private ConcurrentMap<Integer, Transaction> transactions;
	private String sessionId;
	private String participantPrivateId;
	private Long userId;
	private String userUuid;	// the uuid of user
	private String serialNumber;
	private String macAddr;
	private boolean isReconnected;
	private String accessType;

	public RpcConnection(Session session) {
		this.session = session;
		this.transactions = new ConcurrentHashMap<>();
		this.participantPrivateId = session.getSessionId();
	}

	public Session getSession() {
		return session;
	}

	public String getParticipantPrivateId() {
		return participantPrivateId;
	}

	public void setParticipantPrivateId(String participantPrivateId) {
		this.participantPrivateId = participantPrivateId;
	}

	public Long getUserId() { return this.userId; }

	public void setUserId(Long userId) { this.userId = userId; }

	public String getUserUuid() { return this.userUuid; }

	public void setUserUuid(String userUuid) { this.userUuid = userUuid; }

	public String getSerialNumber() { return this.serialNumber; }

	public void setDeviceSerailNumber(String serialNumber) { this.serialNumber = serialNumber; }

	public String getMacAddr() {
		return macAddr;
	}

	public void setMacAddr(String macAddr) {
		this.macAddr = macAddr;
	}

	public boolean isReconnected() {
		return isReconnected;
	}

	public void setReconnected(boolean reconnected) {
		isReconnected = reconnected;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getAccessType() {
		return accessType;
	}

	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	public Transaction getTransaction(Integer transactionId) {
		return transactions.get(transactionId);
	}

	public void addTransaction(Integer transactionId, Transaction t) {
		Transaction oldT = transactions.putIfAbsent(transactionId, t);
		if (oldT != null) {
			log.error("Found an existing transaction for the key {}", transactionId);
		}
	}

	public void removeTransaction(Integer transactionId) {
		transactions.remove(transactionId);
	}

	public Collection<Transaction> getTransactions() {
		return transactions.values();
	}
}
