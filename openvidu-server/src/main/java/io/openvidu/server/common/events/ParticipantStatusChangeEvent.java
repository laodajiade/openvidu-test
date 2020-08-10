package io.openvidu.server.common.events;

import lombok.Builder;
import org.springframework.context.ApplicationEvent;

/**
 * @author chosongi
 * @date 2020/8/10 17:59
 */
@Builder
public class ParticipantStatusChangeEvent extends ApplicationEvent {

    private String uuid;
    private String field;
    private String updateStatus;

    public ParticipantStatusChangeEvent(Object source) {
        super(source);
    }
}
