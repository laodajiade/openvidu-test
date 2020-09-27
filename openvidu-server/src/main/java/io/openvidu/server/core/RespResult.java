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

    public static <T> RespResult<T> ok() {
        RespResult<T> respResult = new RespResult<>();
        respResult.setCode(ErrorCodeEnum.SUCCESS);
        return respResult;
    }

    public static <T> RespResult<T> ok(T result) {
        RespResult<T> respResult = new RespResult<>();
        respResult.setCode(ErrorCodeEnum.SUCCESS);
        respResult.setResult(result);
        return respResult;
    }

//    public static <T extends PageResult, E> RespResult<T> ok(IPage<E> pageInfo) {
//        RespResult<T> respResult = new RespResult<>();
//        respResult.setCode(ErrorCodeEnum.SUCCESS);
//
//        PageResult<E> pageResult = new PageResult<>();
//        pageResult.setList(pageInfo.getRecords());
//        pageResult.setPages(((int) (pageInfo.getPages())));
//        pageResult.setTotal(pageInfo.getTotal());
//        pageResult.setPageNum(((int) (pageInfo.getCurrent())));
//        pageResult.setPageSize(((int) (pageInfo.getSize())));
//        respResult.setResult((T) pageResult);
//        return respResult;
//    }
//
//    public static <T extends PageResult, E> RespResult<T> ok(Collection<E> records, IPage page) {
//        RespResult<T> respResult = new RespResult<>();
//        respResult.setCode(ErrorCodeEnum.SUCCESS);
//
//        PageResult<E> pageResult = new PageResult<>();
//        pageResult.setList(records);
//        pageResult.setPages(((int) (page.getPages())));
//        pageResult.setTotal(page.getTotal());
//        pageResult.setPageNum(((int) (page.getCurrent())));
//        pageResult.setPageSize(((int) (page.getSize())));
//        respResult.setResult((T) pageResult);
//        return respResult;
//    }

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
