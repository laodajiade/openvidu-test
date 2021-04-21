package io.openvidu.server.common.redis;

import io.openvidu.server.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Slf4j
@Component
public class RecordingRedisPublisher {

    private final static String TOPIC_ROOM_RECORDER = "room-recorder";

    private final static String TOPIC_RECORDING_FILE = "recording-file-operation";

    @Resource
    private RedisUtils redisUtils;

    public void send(String topic, String strObj) {
        log.info("Send Topic:{}, Message:{}", topic, strObj);
        try {
            redisUtils.publish(topic, strObj);
            log.info(topic + "消息发送成功:{}", strObj);
        } catch (Exception e) {
            log.info(topic + "消息发送失败:{}异常信息{}", strObj, e.getMessage());
        }
    }

    public void sendRecordingTask(String object) {
        send(TOPIC_ROOM_RECORDER, object);
    }

    public void sendRecordingFileOperationTask(String object) {
        send(TOPIC_RECORDING_FILE, object);
    }
}
