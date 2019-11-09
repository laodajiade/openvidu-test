package io.openvidu.server.common.enums;

import lombok.Getter;

/**
 * @author geedow
 * @date 2019/9/23 10:56
 */
@Getter
public enum InviteLimit {
    NOT_ALLOWABLE(0),
    ALLOWABLE(1);

    private int type;
    InviteLimit(int type) {
        this.type = type;
    }}
