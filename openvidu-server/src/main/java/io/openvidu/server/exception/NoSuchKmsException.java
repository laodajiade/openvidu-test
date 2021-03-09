package io.openvidu.server.exception;

import io.openvidu.server.common.enums.ErrorCodeEnum;

public class NoSuchKmsException extends RuntimeException {

    public NoSuchKmsException() {
        this("没有可用的kms服务");
    }

    public NoSuchKmsException(String message) {
        super(message);
    }
}
