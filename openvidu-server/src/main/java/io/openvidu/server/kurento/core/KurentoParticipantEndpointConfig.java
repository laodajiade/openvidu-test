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

import org.kurento.client.IceComponentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import io.openvidu.server.cdr.CallDetailRecord;
import io.openvidu.server.config.InfoHandler;
import io.openvidu.server.kurento.endpoint.KmsEvent;
import io.openvidu.server.kurento.endpoint.KmsMediaEvent;
import io.openvidu.server.kurento.endpoint.MediaEndpoint;
import org.springframework.beans.factory.annotation.Value;

public class KurentoParticipantEndpointConfig {

	protected static final Logger log = LoggerFactory.getLogger(KurentoParticipantEndpointConfig.class);

	@Value("${leave.room.delay}")
	public long leaveDelay;

	@Autowired
	protected InfoHandler infoHandler;

	@Autowired
	protected CallDetailRecord CDR;

	public void addEndpointListeners(MediaEndpoint endpoint, String typeOfEndpoint) {

//		endpoint.getWebEndpoint().addMediaFlowInStateChangeListener(event -> {
//			String msg = "KMS event [MediaFlowInStateChange] -> endpoint: " + endpoint.getEndpointName() + " ("
//					+ typeOfEndpoint + ") | state: " + event.getState() + " | pad: " + event.getPadName()
//					+ " | mediaType: " + event.getMediaType() + " | timestamp: " + event.getTimestampMillis();
//			KmsEvent kmsEvent = new KmsMediaEvent(event, endpoint.getOwner(), endpoint.getEndpointName(),
//					event.getMediaType(), endpoint.createdAt());
//			endpoint.kmsEvents.add(kmsEvent);
//			this.CDR.log(kmsEvent);
//			this.infoHandler.sendInfo(msg);
//			log.info(msg);
//		});

//		endpoint.getWebEndpoint().addMediaFlowOutStateChangeListener(event -> {
//			String msg = "KMS event [MediaFlowOutStateChange] -> endpoint: " + endpoint.getEndpointName() + " ("
//					+ typeOfEndpoint + ") | state: " + event.getState() + " | pad: " + event.getPadName()
//					+ " | mediaType: " + event.getMediaType() + " | timestamp: " + event.getTimestampMillis();
//			KmsEvent kmsEvent = new KmsMediaEvent(event, endpoint.getOwner(), endpoint.getEndpointName(),
//					event.getMediaType(), endpoint.createdAt());
//			endpoint.kmsEvents.add(kmsEvent);
//			this.CDR.log(kmsEvent);
//			this.infoHandler.sendInfo(msg);
//			log.info(msg);
//		});

//		endpoint.getWebEndpoint().addIceGatheringDoneListener(event -> {
//			String msg = "KMS event [IceGatheringDone] -> endpoint: " + endpoint.getEndpointName() + " ("
//					+ typeOfEndpoint + ") | timestamp: " + event.getTimestampMillis();
//			KmsEvent kmsEvent = new KmsEvent(event, endpoint.getOwner(), endpoint.getEndpointName(),
//					endpoint.createdAt());
//			endpoint.kmsEvents.add(kmsEvent);
//			this.CDR.log(kmsEvent);
//			this.infoHandler.sendInfo(msg);
//			log.info(msg);
//		});

//		endpoint.getWebEndpoint().addConnectionStateChangedListener(event -> {
//			String msg = "KMS event [ConnectionStateChanged]: -> endpoint: " + endpoint.getEndpointName() + " ("
//					+ typeOfEndpoint + ") | oldState: " + event.getOldState() + " | newState: " + event.getNewState()
//					+ " | timestamp: " + event.getTimestampMillis();
//			KmsEvent kmsEvent = new KmsEvent(event, endpoint.getOwner(), endpoint.getEndpointName(),
//					endpoint.createdAt());
//			endpoint.kmsEvents.add(kmsEvent);
//			this.CDR.log(kmsEvent);
//			this.infoHandler.sendInfo(msg);
//			log.info(msg);
//		});

//		endpoint.getWebEndpoint().addNewCandidatePairSelectedListener(event -> {
//			endpoint.selectedLocalIceCandidate = event.getCandidatePair().getLocalCandidate();
//			endpoint.selectedRemoteIceCandidate = event.getCandidatePair().getRemoteCandidate();
//			String msg = "KMS event [NewCandidatePairSelected]: -> endpoint: " + endpoint.getEndpointName() + " ("
//					+ typeOfEndpoint + ") | local: " + endpoint.selectedLocalIceCandidate + " | remote: "
//					+ endpoint.selectedRemoteIceCandidate + " | timestamp: " + event.getTimestampMillis();
//			KmsEvent kmsEvent = new KmsEvent(event, endpoint.getOwner(), endpoint.getEndpointName(),
//					endpoint.createdAt());
//			endpoint.kmsEvents.add(kmsEvent);
//			this.CDR.log(kmsEvent);
//			this.infoHandler.sendInfo(msg);
//			log.info(msg);
//		});

//		endpoint.getEndpoint().addMediaTranscodingStateChangeListener(event -> {
//			String msg = "KMS event [MediaTranscodingStateChange]: -> endpoint: " + endpoint.getEndpointName() + " ("
//					+ typeOfEndpoint + ") | state: " + event.getState().name() + " | mediaType: " + event.getMediaType()
//					+ " | binName: " + event.getBinName() + " | timestamp: " + event.getTimestampMillis();
//			KmsEvent kmsEvent = new KmsMediaEvent(event, endpoint.getOwner(), endpoint.getEndpointName(),
//					event.getMediaType(), endpoint.createdAt());
//			endpoint.kmsEvents.add(kmsEvent);
//			this.CDR.log(kmsEvent);
//			this.infoHandler.sendInfo(msg);
//			log.info(msg);
//		});

		endpoint.getWebEndpoint().addIceComponentStateChangeListener(event -> {
			// if (!event.getState().equals(IceComponentState.READY)) {
			String msg = "KMS event [IceComponentStateChange]: -> endpoint: " + endpoint.getEndpointName() + " ("
					+ typeOfEndpoint + ") | state: " + event.getState().name() + " | componentId: "
					+ event.getComponentId() + " | streamId: " + event.getStreamId() + " | timestamp: "
					+ event.getTimestampMillis();
			log.info(msg);
			KmsEvent kmsEvent = new KmsEvent(event, endpoint.getOwner(), endpoint.getEndpointName(),
					endpoint.createdAt());
			endpoint.kmsEvents.add(kmsEvent);
			this.CDR.log(kmsEvent);
			this.infoHandler.sendInfo(msg);
//			if (event.getState() == IceComponentState.CONNECTED) {
//				endpoint.notifyEndpointPass(typeOfEndpoint);
//			}
			// }
		});

		endpoint.getWebEndpoint().addErrorListener(event -> {
			String msg = "KMS event [ERROR]: -> endpoint: " + endpoint.getEndpointName() + " (" + typeOfEndpoint
					+ ") | errorCode: " + event.getErrorCode() + " | description: " + event.getDescription()
					+ " | timestamp: " + event.getTimestampMillis();
			KmsEvent kmsEvent = new KmsEvent(event, endpoint.getOwner(), endpoint.getEndpointName(),
					endpoint.createdAt());
			endpoint.kmsEvents.add(kmsEvent);
			this.CDR.log(kmsEvent);
			this.infoHandler.sendInfo(msg);
			log.error(msg);
		});
	}

	public CallDetailRecord getCdr() {
		return this.CDR;
	}

}
