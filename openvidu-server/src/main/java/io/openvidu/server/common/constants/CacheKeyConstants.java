package io.openvidu.server.common.constants;

import io.openvidu.server.utils.DateUtil;

import java.util.Date;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/9/12 15:17
 */
public class CacheKeyConstants {

    /** 用户token缓存key前缀 */
    public static final String APP_TOKEN_PREFIX_KEY = "token:";

    /** 用户设备状态缓存key前缀 */
    public static final String DEV_PREFIX_KEY = "device:";

    public static final String CONFERENCE_TO_BEGIN_NOTIFY = "conference:to:begin:notify";

    /** 会议信息key前缀 conference:{sessionId}*/
    private static final String CONFERENCES_KEY = "conference:%s";

    /** 与会人key前缀 participant:{uuid}*/
    private static final String PARTICIPANT_PREFIX_KEY = "participant:%s";

    /** 会议直播信息前缀 conference:living:info:{sessionId} */
    public static final String CONFERENCE_LIVING_INFO_KEY = "conference:living:info:";
    public static final String CONFERENCE_LIVING_INFO_LIVINGURL = "livingUrl";

    public static final String SUBSCRIBER_SET_ROLL_CALL_KEY = "subscriber:setRollCall:%s:%s:%s";

    public static final long DEFAULT_CONFERENCE_EXPIRE = 86400;

    /**
     * 单日最大并发方数缓存key前缀
     */
    public static final String STATISTICS_MAX_DAY_CONCURRENT_PREFIX_KEY = "statistics:max:concurrent:%s:%s";

    /** 设备日志上传token缓存key前缀 */
    public static final String LOG_UPLOAD_TOKEN_PREFIX_KEY = "log:upload:token:";
    /** 会议邀请用户缓存key前缀 */
    private static final String INVITE_PREFIX_KEY = "conference:invite:%s";

    public static final String ACCESSIN_PRIVATEID_PREFIX_KEY = "accessIn:privateId:";

    public static final String CORP_REMAINDER_DURATION_PREFIX_KEY = "remainder:duration:";
    public static final String CORP_ADVANCE_DURATION_PREFIX_KEY = "advance:duration:";
    public static final String CORP_REMAINDER_DURATION_LESSTENHOUR_PREFIX_KEY = "duration:lessenhour:";
    public static final String CORP_REMAINDER_DURATION_USEDUP_PREFIX_KEY = "duration:runout:";

    public static final String MEETING_QUALITY_PREFIX_KEY = "meeting:quality:uuid:";

    public static String getConferencesKey(String sessionId) {
        return String.format(CONFERENCES_KEY, sessionId);
    }

    public static String getParticipantKey(String uuid) {
        return String.format(PARTICIPANT_PREFIX_KEY, uuid);
    }

    public static String getMaxConcurrentStatisticKey(String project, Date date) {
        return String.format(STATISTICS_MAX_DAY_CONCURRENT_PREFIX_KEY,
                DateUtil.getDateFormat(Objects.isNull(date) ? new Date() : date, DateUtil.FORMAT_YEAR_MONTH_DAY), project);
    }
    public static String getSubscriberSetRollCallKey(String sessionId, Long startTime, String uuid) {
        return String.format(SUBSCRIBER_SET_ROLL_CALL_KEY, sessionId, startTime, uuid);
    }

    public static String getConferencesInviteKey(String sessionId) {
        return String.format(INVITE_PREFIX_KEY, sessionId);
    }
}
