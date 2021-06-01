package io.openvidu.server.common.enums;

public enum FixedRoomStatusEnum {

    NOT_ACTIVATION(0),
    USED(1),
    EXPIRED(3);

    int status;

    FixedRoomStatusEnum(int status) {
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
