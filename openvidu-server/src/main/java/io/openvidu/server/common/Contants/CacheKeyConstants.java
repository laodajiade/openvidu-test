package io.openvidu.server.common.Contants;

/**
 * @author chosongi
 * @date 2019/9/12 15:17
 */
public interface CacheKeyConstants {

    String SEPARATOR = ":";

    // 用户token缓存key前缀
    String APP_TOKEN_PREFIX_KEY = "token:";

    // 会议set集合key
    String CONFERENCES_KEY = "conferences";

    // 某会议中的与会人员set集合key前缀
    String PARTICIPANTS_OF_CONFERENCE_PREFIX_KEY = "participants:in:conferences:";

    // 与会人key前缀
    String PARTICIPANT_PREFIX_KEY = "participant:";

}
