package io.openvidu.server.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@AllArgsConstructor
public enum RecordState {
    RECORD_PREPARE(0),
    RECORDING(1),
    RECORD_STOP(2),
    RECORD_EXCEPTION(3);

    @Getter
    private int state;

    public static RecordState parseRecState(int index) {
        return Arrays.stream(RecordState.values()).filter(recordState -> recordState.getState() == index)
                .findAny().orElse(RECORD_STOP);
    }

}
