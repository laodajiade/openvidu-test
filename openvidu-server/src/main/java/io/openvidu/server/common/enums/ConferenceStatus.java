package io.openvidu.server.common.enums;

import lombok.Getter;

/**
 * @author geedow
 * @date 2019/9/23 10:49
 */
@Getter
public enum ConferenceStatus {
    NOT_YET(0),
    PROCESS(1),
    FINISHED(2);

    private int status;
    ConferenceStatus(int status) {
        this.status = status;
    }}
