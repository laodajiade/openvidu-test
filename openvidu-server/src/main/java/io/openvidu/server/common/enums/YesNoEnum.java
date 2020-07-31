package io.openvidu.server.common.enums;

public enum YesNoEnum {
    Y("是"),
    N("否");

    private String desc;

    YesNoEnum(String desc) {
        this.desc = desc;
    }
}
