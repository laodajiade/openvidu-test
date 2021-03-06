package io.openvidu.server.common.enums;

import lombok.Getter;

/**
 * @author geedow
 * @date 2019/9/12 14:25
 */

@Getter
public enum ErrorCodeEnum {
    /*SUCCESS(0, "success"),
    REQUEST_PARAMS_ERROR(10006, "request param error"),

    SERVER_UNKNOWN_ERROR(11000, "server unknown error"),
    SERVER_INTERNAL_ERROR(11001, "server internal error"),
    PERFORMANCE_EXCEED(11002, "performance exceed"),
    INVALID_METHOD_CALL(11003, "invalid method call"),
    UNRECOGNIZED_API(11005, "unrecognized api"),

    TOKEN_INVALID(12003, "token invalid"),
    TOKEN_ERROR(12004, "token error"),

    CONFERENCE_ALREADY_EXIST(13000, "conference already exist"),
    CONFERENCE_NOT_EXIST(13001, "conference not exist"),
    CONFERENCE_IS_LOCKED(13002, "conference is locked"),
    PERMISSION_LIMITED(13004, "permission limited"),
    SHARING_ALREADY_EXIST(13005, "screen sharing already exist"),
    CONFERENCE_PASSWORD_ERROR(13006, "conference password error"),
    CONFERENCE_ALREADY_CLOSED(13007, "conference already closed"),
    USER_NOT_STREAMING_ERROR_CODE(13008, "user not streaming"),
    ROOM_CAPACITY_LIMITED(13009, "room capacity limited"),
    USER_ALREADY_ONLINE(13010, "user already online"),
    CONFERENCE_TOO_LONG(13011, "conference too long"),
    NOT_EXIST_SHARING_FLOW(13012, "not exists sharing flow"),
    SPEAKER_ALREADY_EXIST(13013, "speaker already exist"),
    TERMINAL_MUST_LOGIN_FIRST(13014, "terminal must login first"),
    TERMINAL_IS_NOT_MODERATOR(13015, "terminal is not moderator"),
    DEVICE_NOT_FOUND(13100, "device not found"),
    DEVICE_BUSY(13101,"device busy");*/

    SUCCESS(0, "成功"),
    FAIL(10000, "失败"),
    REQUEST_EXPIRED(10002, "request expired"),
    REQUEST_PARAMS_ERROR(10006, "请求参数错误"),
    CANNOT_BE_OPERATED(10010, "不能如此操作"),
    REQUEST_TOO_FREQUENT(10012,"请求过于频繁"),

    SERVER_UNKNOWN_ERROR(11000, "未知错误"),
    SERVER_INTERNAL_ERROR(11001, "请稍后再试"),
//    SERVER_INTERNAL_ERROR(11001, "服务内部错误"),
    PERFORMANCE_EXCEED(11002, "服务不给力，请稍后再试"),
    INVALID_METHOD_CALL(11003, "非法调用"),
    UNRECOGNIZED_API(11005, "非法API调用"),
    CORP_SERVICE_EXPIRED(11006, "服务已到期"),
    ACCESS_IN_NEEDED(11007, "重新链接服务器中"),
    RATE_LIMITER(11011, "请稍后再试"),
    VERSION_LOW(11012, "当前服务暂不支持，请对平台服务进行升级"),

    TOKEN_INVALID(12003, "token已失效"),
    TOKEN_ERROR(12004, "token错误"),
    USER_NOT_EXIST(12005, "用户不存在"),
    ORIGINAL_PASSWORD_ERROR(12006, "用户原始密码错误"),
    INCORRECT_FORMAT_PASSWORD(12031, "密码格式错误"),

    CONFERENCE_ALREADY_EXIST(13000, "会议已存在"),
    CONFERENCE_NOT_EXIST(13001, "会议不存在"),
    CONFERENCE_IS_LOCKED(13002, "会议已锁定"),
    CONFERENCE_IS_FINISHED(13002, "会议已结束"),
    PERMISSION_LIMITED(13004, "没有操作权限"),
    SHARING_ALREADY_EXIST(13005, "共享已存在"),
    CONFERENCE_PASSWORD_ERROR(13006, "会议密码错误"),
    CONFERENCE_ALREADY_CLOSED(13007, "会议已关闭"),
    USER_NOT_STREAMING_ERROR_CODE(13008, "用户未发布视频流"),
    ROOM_CAPACITY_LIMITED(13009, "会议房间容量超限"),
    USER_ALREADY_ONLINE(13010, "用户已在线"),
    CONFERENCE_TOO_LONG(13011, "会议时长超出限制"),
    NOT_EXIST_SHARING_FLOW(13012, "共享不存在"),
    SPEAKER_ALREADY_EXIST(13013, "会议中已存在发言者"),
    TERMINAL_MUST_LOGIN_FIRST(13014, "硬终端须先登录"),
    TERMINAL_IS_NOT_MODERATOR(13015, "硬终端不是主持人"),
    WEB_MODERATOR_ALREADY_EXIST(13016, "会议中已存在web会控"),
    PARTICIPANT_NOT_FOUND(13017, "与会者已离会"),
    APPOINT_CONFERENCE_CONFLICT(13019, "预约会议时间冲突"),
    START_TIME_LATE(13020, "会议开始时间不能早于现在时间"),
    DURATION_TOO_SHORT(13022, "会议时长必须大于0"),
    CONFERENCE_RECORD_NOT_EXIST(13025, "会议记录不存在"),
    CONFERENCE_RECORD_NOT_START(13027, "会议尚未开始录制"),
    CONFERENCE_IS_RECORDING(13028, "会议正在录制中"),
    SHARING_ALREADY_EXISTS(13029, "会议中已存在共享"),
    CONFERENCE_IS_LIVING(13031, "会议正在直播中"),
    CONFERENCE_LIVE_NOT_START(13032, "会议尚未开始直播"),
    COUNT_OF_CONFERENCE_LIMIT(13033,"会议数已达上限"),
    CONFERENCE_RECORD_FREQUENT_OPERATION(13034, "会议录制操作频繁，请稍后再试"),
    JOIN_ROOM_DUPLICATELY(13035, "重复入会"),
    MODERATOR_PROHIBIT_ON_SPEAKER(13039,"主持人禁止开启扬声器"),
    ROOM_CAPACITY_CORP_LIMITED(13041, "已达企业会议人数上限，请稍后再试"),
    ROOM_CAPACITY_PERSONAL_LIMITED(13042, "已达会议人数上限，请稍后再试"),
    PARTICIPANT_DOWN_HAND_NOW(13043, "参会者已取消举手发言"),
    THE_CONFERENCE_HAS_STARTED(13048, "预约会议已开始"),
    THE_POLLING_HAS_STARTED(13049,"轮询已经开启"),
    RECORD_STORAGE_EXHAUSTED(13050, "存储容量不足，暂不可录制会议"),
    APPOINTMENT_TIME_AFTER_SERVICE_EXPIRED(13051, "预约会议时间不可超出服务到期时间"),
    RECORD_STORAGE_NOT_ENOUGH(13052, "录制存储剩余容量少于100MB"),
    CANNOT_START_POLLING(13053,"不满足开启轮询条件"),
    APPOINTMENT_CONFERENCE_NOT_EXIST(13054,"预约会议不存在"),
    APPOINTMENT_CONFERENCE_HAS_FINISHED(13055,"预约会议已结束"),
    APPOINTMENT_CONFERENCE_DID_NOT_START(13056,"会议未开始"),
    JOIN_ROOM_NEEDED(13057,"请先加入房间"),
    REMAINDER_DURATION_USE_UP(13058,"通话时长已用完，请联系管理员"),
    UPLOAD_MEETING_QUALITY_TIMEOUT(13059,"会议质量上报超时"),
    UNSUPPORTED_RECORD_OPERATION(13061,"当前不支持录制服务"),
    RECORD_SERVICE_INVALID(13062,"录制服务已过期，请联系管理员"),
    SET_ROLL_CALL_SAME_PART(13063,"不能点名同一个人"),
    MODERATOR_NOT_FOUND(13065,"主持人已离会，请稍后再试"),
    IM_ALL_LIMIT(13066,"全体禁言"),
    IM_ONLY_PUBLISH(13067,"主持人不允许私聊"),
    IM_ONLY_TO_MODERATOR(13068,"仅允许与主持人私聊"),
    POLLING_IS_NOT_STARTED(13069, "轮询尚未开始"),
    NO_RECORD_SERVICE(13070, "未开通录制服务，请联系管理员"),
    SENSITIVE_WORD(13071, "晨浩憨憨"),
    ILLEGAL_VERSION(13072, "非法版本号"),
    SIP_CANNOT_BE_A_SPEAKER(13074	, "sip不能成为发言者"),
    FIXED_ROOM_EXPIRED(13076	, "固定会议室已到期"),
    ROOM_IS_IN_USE(13077, "该会议号已有正在进行的会议"),
    REPEAT_ADD_CONTACTS(13078, "已经是常用联系人,请勿重复添加"),
    REPEAT_DEL_CONTACTS(13079, "此用户不是常用联系人,删除失败"),
    JOIN_ROOM_TIMEOUT(13080, "请稍后再试"),
    APPOINTMENT_STATUS_ERROR(13081, "预约会议状态错误"),
    SERVICE_NOT_ACTIVATION_OR_EXPIRED(13082, "并发服务未生效或已到期"),
    NOT_RECORDING_PERMISSION(13083,"暂无权限"),
    OTHER_RECORDING_LATER_RETRY(13084,"已有其他会议正在录制,请稍后重试"),
    APPOINTMENT_TIME_AFTER_ROOM_EXPIRED(13085, "预约会议时间不可超出固定会议室到期时间"),
    NOT_ROOM_MANAGER(13086, "不是房间管理员"),
    PRE_CONFERENCE_NOT_FINISHED(13087, "上个会议仍未结束，请等待"),
    ENP_POINT_NAME_NOT_EXIST(13088, "endPointName不存在"),
    MUST_SHARING_ROLE(13089, "必须是分享角色"),
    NOT_SUPPORT_RECORDING_SERVER(13090,"当前不支持录制服务"),
    NOT_SUPPORT_SIP_REGISTEE(13091,"当前不支持SIP注册服务"),
    NOT_SUPPORT_H323_REGISTEE(13092,"当前不支持H323注册服务"),
    NOT_SUPPORT_LIVEING_SERVER(13093,"当前不支持直播服务"),
    NOT_SUPPORT_TRAVERSING_SERVER(13094,"当前不支持穿透服务"),
    RECORDING_SERVER_UPPER_LIMIT(13095,"已达企业录制会议数量上限,请稍后再试"),

    DEVICE_NOT_FOUND(13100, "设备不存在"),
    DEVICE_BUSY(13101,"设备在会议或升级中");

    private int code;
    private String message;
    ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
