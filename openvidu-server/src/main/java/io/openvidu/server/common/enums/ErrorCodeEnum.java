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
    REQUEST_PARAMS_ERROR(10006, "请求参数错误"),

    SERVER_UNKNOWN_ERROR(11000, "未知错误"),
    SERVER_INTERNAL_ERROR(11001, "服务内部错误"),
    PERFORMANCE_EXCEED(11002, "服务不给力，请稍后再试"),
    INVALID_METHOD_CALL(11003, "非法调用"),
    UNRECOGNIZED_API(11005, "非法API调用"),

    TOKEN_INVALID(12003, "token已失效"),
    TOKEN_ERROR(12004, "token错误"),

    CONFERENCE_ALREADY_EXIST(13000, "会议已存在"),
    CONFERENCE_NOT_EXIST(13001, "会议不存在"),
    CONFERENCE_IS_LOCKED(13002, "会议已锁定"),
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
    DEVICE_NOT_FOUND(13100, "设备不存在"),
    DEVICE_BUSY(13101,"设备在会议或升级中");

    private int code;
    private String message;
    ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
