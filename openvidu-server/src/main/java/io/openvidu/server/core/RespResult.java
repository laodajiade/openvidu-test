package io.openvidu.server.core;

import io.openvidu.server.common.enums.ErrorCodeEnum;
import lombok.Data;

@Data
public class RespResult<T> {
    ErrorCodeEnum code;
    T result;

    Notification notification;


    public static <T> RespResult<T> fail(ErrorCodeEnum code) {
        RespResult<T> respResult = new RespResult<>();
        respResult.setCode(code);
        return respResult;
    }

    public static <T> RespResult<T> fail(ErrorCodeEnum code,T result) {
        RespResult<T> respResult = new RespResult<>();
        respResult.setCode(code);
        respResult.setResult(result);
        return respResult;
    }

    public static <T> RespResult<T> ok() {
        RespResult<T> respResult = new RespResult<>();
        respResult.setCode(ErrorCodeEnum.SUCCESS);
        return respResult;
    }

    public static <T> RespResult<T> end(ErrorCodeEnum code) {
        RespResult<T> respResult = new RespResult<>();
        respResult.setCode(code);
        return respResult;
    }

    public static <T> RespResult<T> ok(T result) {
        RespResult<T> respResult = new RespResult<>();
        respResult.setCode(ErrorCodeEnum.SUCCESS);
        respResult.setResult(result);
        return respResult;
    }



    public static <T> RespResult<T> ok(T result, Notification notification) {
        RespResult<T> respResult = new RespResult<>();
        respResult.setCode(ErrorCodeEnum.SUCCESS);
        respResult.setResult(result);
        respResult.setNotification(notification);
        return respResult;
    }

    public boolean isOk() {
        return code == ErrorCodeEnum.SUCCESS;
    }
}
