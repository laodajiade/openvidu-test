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
//    DEFAULT(0),
    ONE(1),
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    ELEVEN(11),
    TWELVE(12),
    THIRTEEN(13);

    private int mode;

    public static LayoutModeEnum getLayoutMode(int layoutMode) {
        return Stream.of(LayoutModeEnum.values()).filter(
                layoutModeEnum -> layoutModeEnum.getMode() == layoutMode).findAny().orElse(null);
    }
}
