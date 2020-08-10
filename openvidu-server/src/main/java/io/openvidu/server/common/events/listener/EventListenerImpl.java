package io.openvidu.server.common.events.listener;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.events.ParticipantStatusChangeEvent;
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

    private static final Gson gson = new GsonBuilder().create();

    @EventListener
    @Async
    public void updateParticipantStatus(ParticipantStatusChangeEvent event) {
        log.info("update participant status:", event.getSource());
        JsonObject jsonObject = gson.fromJson(event.getSource().toString(), JsonObject.class);
        cacheManage.updatePartInfo(jsonObject.get("uuid").getAsString(),
                jsonObject.get("field").getAsString(), jsonObject.get("status").getAsString());
    }

}
