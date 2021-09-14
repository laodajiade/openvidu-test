package io.openvidu.server.common.events;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StatusEvent {
    private String sessionId;
    private String uuid;
    private String field;
    private Object updateStatus;

    @Override
    public String toString() {
        return "uuid:" + uuid + ", field:" + field + ", updateStatus:" + updateStatus;
    }
}
