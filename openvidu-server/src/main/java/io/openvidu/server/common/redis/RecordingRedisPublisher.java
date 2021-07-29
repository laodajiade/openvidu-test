package io.openvidu.server.common.redis;

import io.openvidu.server.common.cache.CacheManage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Slf4j
@Component
public class RecordingRedisPublisher {

    private final static String TOPIC_ROOM_RECORDER = "room-recorder";

    private final static String TOPIC_RECORDING_FILE = "recording-file-operation";

    @Resource
    private CacheManage cacheManage;

    public void send(String topic, String strObj) {
        log.info("Send Topic:{}, Message:{}", topic, strObj);
        try {
            cacheManage.publish(topic, strObj);
        } catch (Exception e) {
            log.error(topic + "send Topic error:{}", strObj, e);
        }
    }

    public void sendRecordingTask(String object) {
        send(TOPIC_ROOM_RECORDER, object);
    }

    public void sendRecordingFileOperationTask(String object) {
        send(TOPIC_RECORDING_FILE, object);
    }
}
