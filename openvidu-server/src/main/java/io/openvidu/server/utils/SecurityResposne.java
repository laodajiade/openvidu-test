package io.openvidu.server.utils;


import lombok.Data;

/**
 * @author
 * @version V1.0
 * @Title:
 * @Package
 * @Description: 自定义响应数据结构
 */
@Data
public class SecurityResposne {


    // 响应业务状态
    private Integer code;

    // 响应消息
    private String msg;

    // 响应中的数据
    private Object data;


    public static SecurityResposne ok(Object data) {
        return new SecurityResposne(data);
    }

    public static SecurityResposne ok() {
        return new SecurityResposne(null);
    }


    public static SecurityResposne errorMsg(Integer code, String msg) {
        return new SecurityResposne(code, msg, null);
    }


    public static SecurityResposne errorException(String msg) {
        return new SecurityResposne(555, msg, null);
    }


    public SecurityResposne() {

    }

    public SecurityResposne(Integer status, String msg, Object data) {
        this.code = status;
        this.msg = msg;
        this.data = data;
    }


    public SecurityResposne(Object data) {
        this.code = 200;
        this.msg = "OK";
        this.data = data;
    }


}
