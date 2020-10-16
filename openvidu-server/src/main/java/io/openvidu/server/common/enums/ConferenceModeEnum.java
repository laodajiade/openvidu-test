package io.openvidu.server.common.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

public enum ConferenceModeEnum {
    SFU(0),
    MCU(1);

    @Getter
    private int mode;

    ConferenceModeEnum(int mode) {
        this.mode = mode;
    }

    public static ConferenceModeEnum parse(Integer mode) {
        return Objects.isNull(mode) ? ConferenceModeEnum.SFU : Arrays.stream(ConferenceModeEnum.values()).filter(conferenceModeEnum ->
                conferenceModeEnum.getMode() == mode).findAny().orElse(ConferenceModeEnum.SFU);
    }
}
