package io.openvidu.server.common.enums;

import java.util.Arrays;

public enum IMModeEnum {


    ALL_LIMIT(0),
    NOT_LIMIT(1),
    ONLY_PUBLISH(2),
    ONLY_TO_MODERATOR(3),
    ;

    private int mode;

    IMModeEnum(int type) {
        this.mode = type;
    }

    public static IMModeEnum parse(int mode) {
        return Arrays.stream(IMModeEnum.values()).filter(modeEnum -> modeEnum.mode == mode)
                .findAny().orElseThrow(() -> new IllegalArgumentException("im mode not found"));
    }

    public int getMode() {
        return mode;
    }

}
