package io.openvidu.server.exception;

import io.openvidu.server.common.enums.ErrorCodeEnum;

public class BizException extends RuntimeException {

    private ErrorCodeEnum errorCodeEnum;

    public BizException(ErrorCodeEnum errorCodeEnum) {
        this.errorCodeEnum = errorCodeEnum;
    }

    public BizException(ErrorCodeEnum errorCodeEnum, String message) {
        super(message);
        this.errorCodeEnum = errorCodeEnum;
    }

    public ErrorCodeEnum getRespEnum() {
        return errorCodeEnum;
    }

    public int getCode() {
        return errorCodeEnum.getCode();
    }

}
