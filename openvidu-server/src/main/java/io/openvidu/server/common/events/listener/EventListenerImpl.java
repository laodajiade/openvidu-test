package io.openvidu.server.common.events.listener;

import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.events.ParticipantStatusChangeEvent;
import io.openvidu.server.common.events.StatusEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author chosongi
 * @date 2020/8/10 18:02
 */
@Slf4j
@Component
@EnableAsync
public class EventListenerImpl {

    @Resource
    private CacheManage cacheManage;

    @EventListener
    @Async
    public void updateParticipantStatus(ParticipantStatusChangeEvent event) {
        StatusEvent statusEvent = (StatusEvent) event.getSource();
        log.info("update participant status:", statusEvent.toString());
        cacheManage.updatePartInfo(statusEvent.getUuid(), statusEvent.getField(), statusEvent.getUpdateStatus());
    }

}