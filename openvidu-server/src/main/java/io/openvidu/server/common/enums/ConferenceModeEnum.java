package io.openvidu.server.common.enums;

import lombok.Getter;

public enum ConferenceModeEnum {
    SFU(0),
    MCU(1);

    @Getter
    private int mode;

    ConferenceModeEnum(int mode) {
        this.mode = mode;
    }
}
