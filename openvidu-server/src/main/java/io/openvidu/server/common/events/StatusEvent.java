package io.openvidu.server.common.events;

import lombok.Builder;

@Builder
public class StatusEvent {
    private String uuid;
    private String field;
    private String updateStatus;
}
