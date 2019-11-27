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

package io.openvidu.java.client;

import java.util.*;

/**
 * See {@link io.openvidu.java.client.TokenOptions.Builder#role(OpenViduRole)}
 */
public enum OpenViduRole {

	/**
	 * Can subscribe to published Streams of other users
	 */
	SUBSCRIBER,

	/**
	 * SUBSCRIBER permissions + can publish their own Streams (call
	 * <code>Session.publish()</code>)
	 */
	PUBLISHER,

	/**
	 * SUBSCRIBER + PUBLISHER permissions + can force the unpublishing or
	 * disconnection over a third-party Stream or Connection (call
	 * <code>Session.forceUnpublish()</code> and
	 * <code>Session.forceDisconnect()</code>)
	 */
	MODERATOR,

	/**
	 * SUBSCRIBER permissions + can force the unpublishing or
	 * disconnection over a third-party Stream or Connection (call
	 * <code>Session.forceUnpublish()</code> and
	 * <code>Session.forceDisconnect()</code>)
	 */
	THOR;

	public static final List<OpenViduRole> NON_PUBLISH_ROLES;

	public static final List<OpenViduRole> MODERATOR_ROLES;


	static {
		NON_PUBLISH_ROLES = new ArrayList<OpenViduRole>(2) {{
			add(OpenViduRole.SUBSCRIBER);
			add(OpenViduRole.THOR);
		}};

		MODERATOR_ROLES = new ArrayList<OpenViduRole>(2) {{
			add(OpenViduRole.MODERATOR);
			add(OpenViduRole.THOR);
		}};
	}

	public static OpenViduRole parseRole(String role) {
		for (OpenViduRole openViduRole : OpenViduRole.values()) {
			if (openViduRole.name().equalsIgnoreCase(role)) return openViduRole;
		}
		return null;
	}

}
