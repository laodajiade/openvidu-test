package io.openvidu.server.common.events;

import org.springframework.context.ApplicationEvent;

/**
 * @author chosongi
 * @date 2020/8/10 17:59
 */
public class ParticipantStatusChangeEvent extends ApplicationEvent {

    public ParticipantStatusChangeEvent(Object source) {
        super(source);
    }

}
