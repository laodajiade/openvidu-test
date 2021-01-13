package io.openvidu.server.core;

/**
 * @author even
 * @date 2021/1/13 18:23
 */
public enum JpushMsgEnum {

    MEETING_INVITE("0"),
    MEETING_NOTIFY("1");

    private String message;

    JpushMsgEnum(String message){
        this.message = message;
    }

}
