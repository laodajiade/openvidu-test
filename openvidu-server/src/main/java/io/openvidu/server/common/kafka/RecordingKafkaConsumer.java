package io.openvidu.server.common.kafka;

import io.openvidu.server.core.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

/**
 * @author chosongi
 * @date 2020/9/10 20:18
 */
@Slf4j
@Component
public class RecordingKafkaConsumer {

    @Resource
    private SessionManager sessionManager;

    private static final String TOPIC_ROOM_RECORDER_ERROR = "recording-error";

    @KafkaListener(topics = {TOPIC_ROOM_RECORDER_ERROR})
    public void topicRoomRecorderFile(ConsumerRecord<?, ?> record) {
        Optional message = Optional.ofNullable(record.value());
        if (message.isPresent()) {
            Object msg = message.get();
            log.info("Recv recording file operation. Topic:{}, Message:{}", record.topic(), msg);
            sessionManager.handleRecordErrorEvent(msg);
        }
    }
}
