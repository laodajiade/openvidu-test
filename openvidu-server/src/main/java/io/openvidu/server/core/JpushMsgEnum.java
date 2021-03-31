package io.openvidu.server.core;

import lombok.Getter;

/**
 * @author even
 * @date 2021/1/13 18:23
 */
public enum JpushMsgEnum {

    MEETING_INVITE("0"),
    MEETING_NOTIFY("1");

    @Getter
    private String message;

    JpushMsgEnum(String message){
        this.message = message;
    }

}
