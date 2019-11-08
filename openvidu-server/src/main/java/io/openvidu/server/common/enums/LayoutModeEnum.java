package io.openvidu.server.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

/**
 * @author geedow
 * @date 2019/11/7 15:30
 */
@Getter
@AllArgsConstructor
public enum LayoutModeEnum {
    DEFAULT(0),
    FOUR(4),
    SIX(6),
    NINE(9),
    TWELVE(12);

    private int mode;

    public static LayoutModeEnum getLayoutMode(int layoutMode) {
        return Stream.of(LayoutModeEnum.values()).filter(
                layoutModeEnum -> layoutModeEnum.getMode() == layoutMode).findAny().orElse(null);
    }
}
