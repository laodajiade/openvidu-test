package io.openvidu.server.common.enums;

import lombok.Getter;

public enum TerminalTypeEnum {
    HDC("HDC"),
    A("Android"),
    I("IOS"),
    W("Windows"),
    M("Mac"),
    S("SIP"),
    WEB("WEB");

    @Getter
    private String desc;

    TerminalTypeEnum(String desc) {
        this.desc = desc;
    }
}
