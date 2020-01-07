package io.openvidu.server.common.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum OperateEnum {
    moveUp(1),
    moveDown(2),
    moveLeft(3),
    moveRight(4),
    amplification(5),
    shrink(6);

    private int code;

    public static OperateEnum getLayoutMode(int code) {
        return Stream.of(OperateEnum.values()).filter(
                operateEnum -> operateEnum.getCode() == code).findAny().orElse(null);
    }
}
