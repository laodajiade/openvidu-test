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

package io.openvidu.server.core;

import com.google.gson.JsonObject;
import io.openvidu.java.client.SessionProperties;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

public interface SessionInterface {

	String getSessionId();

	SessionProperties getSessionProperties();

	void join(Participant participant);

	void leave(String participantPrivateId, EndReason reason);

	void leaveRoom(Participant p, EndReason reason);

	boolean close(EndReason reason);

	boolean isClosed();

	Set<Participant> getParticipants();

	Participant getParticipantByPrivateId(String participantPrivateId);

	Participant getParticipantByPublicId(String participantPublicId);

	//Map<String, Participant> getSameAccountParticipants(String userUuid);

	int getActivePublishers();

	JsonObject toJson();

	JsonObject withStatsToJson();

	Long getStartTime();

    ConcurrentMap<String, Participant> getSamePrivateIdParts(String participantPrivateId);

	boolean setIsRecording(boolean flag);

	boolean sessionAllowedStartToRecord();

	boolean sessionAllowedToStopRecording();

	boolean setIsLiving(boolean flag);

	boolean sessionAllowedStartToLive();

	boolean sessionAllowedToStopLiving();

	Map<String, String> getLayoutRelativePartId();

}
