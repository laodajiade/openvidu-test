package io.openvidu.server.common.enums;

import lombok.Getter;

import java.util.Arrays;

public enum ParticipantStatusEnum {
    PROCESS(0),
    LEAVE(1);

    @Getter
    private int status;

    ParticipantStatusEnum(int status) {
        this.status = status;
    }

    public static ParticipantStatusEnum parse(int status) {
        return Arrays.stream(ParticipantStatusEnum.values()).filter(conferenceModeEnum ->
                conferenceModeEnum.getStatus() == status).findAny().orElse(null);
    }
}
