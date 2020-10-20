package io.openvidu.server.common.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.Resource;

/**
 * @author chosongi
 * @date 2020/9/8 10:56
 */
@Slf4j
@Component
public class RecordingKafkaProducer {

    @Value("${kafka.topic.room.recorder}")
    private String TOPIC_ROOM_RECORDER;

    @Value("${kafka.topic.recording.file}")
    private String TOPIC_RECORDING_FILE;

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String topic, String strObj) {
        log.info("Send Topic:{}, Message:{}", topic, strObj);
        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, strObj);
        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
            @Override
            public void onFailure(Throwable throwable) {
                log.info(topic + " - send msg fatal:" + throwable.getMessage());
            }

            @Override
            public void onSuccess(SendResult<String, Object> stringObjectSendResult) {
                log.info(topic + " - send msg succeed:" + stringObjectSendResult.toString());
            }
        });
    }

    public void sendRecordingTask(String object) {
        send(TOPIC_ROOM_RECORDER, object);
    }

    public void sendRecordingFileOperationTask(String object) {
        send(TOPIC_RECORDING_FILE, object);
    }

}
