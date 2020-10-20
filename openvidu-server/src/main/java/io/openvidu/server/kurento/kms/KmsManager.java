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

package io.openvidu.server.kurento.kms;

import com.google.gson.JsonObject;
import io.openvidu.server.common.manage.KmsRegistrationManage;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.SessionManager;
import org.kurento.client.KurentoConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class KmsManager {

	public class KmsLoad implements Comparable<KmsLoad> {

		private Kms kms;
		private double load;

		public KmsLoad(Kms kms, double load) {
			this.kms = kms;
			this.load = load;
		}

		public Kms getKms() {
			return kms;
		}

		public double getLoad() {
			return load;
		}

		@Override
		public int compareTo(KmsLoad o) {
			return Double.compare(this.load, o.load);
		}

		public JsonObject toJson() {
			JsonObject json = this.kms.toJson();
			json.addProperty("load", this.load);
			return json;
		}

		public JsonObject toJsonExtended(boolean withSessions, boolean withExtraInfo) {
			JsonObject json = this.kms.toJsonExtended(withSessions, withExtraInfo);
			json.addProperty("load", this.load);
			return json;
		}

	}

	protected static final Logger log = LoggerFactory.getLogger(KmsManager.class);

	@Autowired
	protected OpenviduConfig openviduConfig;

	@Autowired
	protected LoadManager loadManager;

	@Autowired
	protected KmsRegistrationManage kmsRegistrationManage;

	@Autowired
	private SessionManager sessionManager;

	@Value("${openvidu.load.kms.interval}")
	private long loadKmsInterval;

	final protected Map<String, Kms> kmss = new ConcurrentHashMap<>();

	public synchronized void addKms(Kms kms) {
		this.kmss.put(kms.getId(), kms);
	}

	public synchronized Kms removeKms(String kmsId) {
		return this.kmss.remove(kmsId);
	}

	public synchronized Kms getLessLoadedKms() throws NoSuchElementException {
		return Collections.min(getKmsLoads()).kms;
	}

	public Kms getKms(String kmsId) {
		return this.kmss.get(kmsId);
	}

	public boolean kmsWithUriExists(String kmsUri) {
		return this.kmss.values().stream().anyMatch(kms -> kms.getUri().equals(kmsUri));
	}

	public KmsLoad getKmsLoad(String kmsId) {
		Kms kms = this.kmss.get(kmsId);
		return new KmsLoad(kms, kms.getLoad());
	}

	public Collection<Kms> getKmss() {
		return this.kmss.values();
	}

	public synchronized List<KmsLoad> getKmssSortedByLoad() {
		List<KmsLoad> kmsLoads = getKmsLoads();
		Collections.sort(kmsLoads);
		return kmsLoads;
	}

	private List<KmsLoad> getKmsLoads() {
		ArrayList<KmsLoad> kmsLoads = new ArrayList<>();
		for (Kms kms : kmss.values()) {
			double load = kms.getLoad();
			kmsLoads.add(new KmsLoad(kms, load));
			log.trace("Calc load {} for kms {}", load, kms.getUri());
		}
		return kmsLoads;
	}

	public boolean destroyWhenUnused() {
		return false;
	}

	protected KurentoConnectionListener generateKurentoConnectionListener(String kmsId) {
		return new KurentoConnectionListener() {

			@Override
			public void reconnected(boolean sameServer) {
				final Kms kms = kmss.get(kmsId);
				kms.setKurentoClientConnected(true);
				kms.setTimeOfKurentoClientConnection(System.currentTimeMillis());
				if (!sameServer) {
					// Different KMS. Reset sessions status (no Publisher or SUbscriber endpoints)
					log.warn("Kurento Client reconnected to a different KMS instance, with uri {}", kms.getUri());
					log.warn("Updating all webrtc endpoints for active sessions");
					final long timeOfKurentoDisconnection = kms.getTimeOfKurentoClientDisconnection();
					kms.getKurentoSessions().forEach(kSession -> {
						kSession.restartStatusInKurento(timeOfKurentoDisconnection);
					});
				} else {
					// Same KMS. We may infer that openvidu-server/KMS connection has been lost, but
					// not the clients/KMS connections
					log.warn("Kurento Client reconnected to same KMS {} with uri {}", kmsId, kms.getUri());
				}
				kms.setTimeOfKurentoClientDisconnection(0);
			}

			@Override
			public void disconnected() {
				final Kms kms = kmss.get(kmsId);
				kms.setKurentoClientConnected(false);
				kms.setTimeOfKurentoClientDisconnection(System.currentTimeMillis());
				log.warn("Kurento Client disconnected from KMS {} with uri {}", kmsId, kms.getUri());

				// TODO 重构KMS的重连，排查disconnected原因
				/*kms.getKurentoSessions().forEach(kSession ->
						sessionManager.updateRoomAndPartInfoAfterKMSDisconnect(kSession.getSessionId()));*/
			}

			@Override
			public void connectionFailed() {
				final Kms kms = kmss.get(kmsId);
				kms.setKurentoClientConnected(false);
				log.warn("Kurento Client failed connecting to KMS {} with uri {}", kmsId, kms.getUri());
			}

			@Override
			public void connected() {
				final Kms kms = kmss.get(kmsId);
				kms.setKurentoClientConnected(true);
				kms.setTimeOfKurentoClientConnection(System.currentTimeMillis());
				log.warn("Kurento Client is now connected to KMS {} with uri {}", kmsId, kms.getUri());
			}
		};
	}

	public abstract List<Kms> initializeKurentoClients(List<String> kmsUris);

	public abstract Kms initializeClient(String kmsUri);

	@PostConstruct
	private void postConstruct() {
		this.initializeKurentoClients(this.openviduConfig.getKmsUris());
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleAtFixedRate(() -> {
			List<String> regRecentKms = kmsRegistrationManage.getRecentRegisterKms();
			if (!CollectionUtils.isEmpty(regRecentKms)) {
				regRecentKms.forEach(kmsUri -> {
					try {
						initializeClient(kmsUri);
					} catch (Exception e) {
						log.error("Recent register KMS in {} is not reachable by OpenVidu Server", kmsUri);
					}
				});
			}
		}, loadKmsInterval, loadKmsInterval, TimeUnit.SECONDS);
	}

}
