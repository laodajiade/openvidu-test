package io.openvidu.server.common.contants;

import io.openvidu.server.common.enums.StreamType;

/**
 * @author geedow
 * @date 2019/9/12 15:17
 */
public class CacheKeyConstants {

    /** 用户token缓存key前缀 */
    public static final String APP_TOKEN_PREFIX_KEY = "token:";

    /** 会议信息key前缀 conference:{sessionId}*/
    public static final String CONFERENCES_KEY = "conference:%s";

    /** 与会人员set集合key前缀 participants:in:conference:{sessionId}*/
    public static final String PARTICIPANTS_OF_CONFERENCE_PREFIX_KEY = "participants:in:conference:%s";

    /** 与会人key前缀 participant:{privateId}:{streamType}*/
    public static final String PARTICIPANT_PREFIX_KEY = "participant:%s:%s";


    public static String getConferencesKey(String sessionId) {
        return String.format(CONFERENCES_KEY, sessionId);
    }

    public static String getParticipantsCollectionKey(String sessionId) {
        return String.format(PARTICIPANTS_OF_CONFERENCE_PREFIX_KEY, sessionId);
    }

    public static String getParticipantKey(String privateId, StreamType streamType) {
        return String.format(PARTICIPANT_PREFIX_KEY, privateId, streamType.name());
    }
}
