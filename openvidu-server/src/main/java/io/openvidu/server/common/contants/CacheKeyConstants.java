package io.openvidu.server.common.contants;

/**
 * @author chosongi
 * @date 2019/9/12 15:17
 */
public interface CacheKeyConstants {

    // 用户token缓存key前缀
    String APP_TOKEN_PREFIX_KEY = "token:";

    // 会议信息key前缀 conference:{sessionId}
    String CONFERENCES_KEY = "conference:%s";

    // 与会人员set集合key前缀 participants:in:conference:{sessionId}
    String PARTICIPANTS_OF_CONFERENCE_PREFIX_KEY = "participants:in:conference:%s";

    // 与会人key前缀 participant:{privateId}:{streamType}
    String PARTICIPANT_PREFIX_KEY = "participant:%s:%s";
}
