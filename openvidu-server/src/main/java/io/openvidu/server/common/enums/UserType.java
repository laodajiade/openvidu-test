package io.openvidu.server.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @author chosongi
 * @date 2020/3/16 20:35
 */
@AllArgsConstructor
public enum UserType {
    register(0),
    tourist(1);

    @Getter
    private int type;

    public static UserType parse(int type) {
        return Arrays.stream(UserType.values()).filter(userType -> userType.getType() == type)
                .findAny().orElse(null);
    }
}
