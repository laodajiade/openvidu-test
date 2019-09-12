package io.openvidu.server.common.enums;

import lombok.Getter;

/**
 * @author chosongi
 * @date 2019/9/12 14:25
 */

@Getter
public enum ErrorCodeEnum {
    REQUEST_PARAMS_ERROR(10006, "request param error"),

    SERVER_UNKNOWN_ERROR(11000, "server unknown error"),
    SERVER_INTERNAL_ERROR(11001, "server internal error"),
    PERFORMANCE_EXCEED(11002, "performance exceed"),

    TOKEN_INVALID(12003, "token invalid"),
    TOKEN_ERROR(12004, "token error");


    private int code;
    private String message;
    ErrorCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
