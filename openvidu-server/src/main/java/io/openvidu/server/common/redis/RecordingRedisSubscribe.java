package io.openvidu.server.common.redis;

import com.alibaba.fastjson.JSONObject;
import io.openvidu.server.recording.service.RecordingManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class RecordingRedisSubscribe {

    private CountDownLatch latch;

    @Autowired
    public RecordingRedisSubscribe(CountDownLatch latch) {
        this.latch = latch;
    }

    @Resource
    private RecordingManager recordingManager;

    /**
     * 队列消息接收方法
     *
     * @param jsonMsg
     */

    public void receiveMessage(String jsonMsg) {
        log.info("[开始消费REDIS消息队列phone数据...]");
        try {
            log.info("监听者收到消息：{}", jsonMsg);
            JSONObject exJson = JSONObject.parseObject(jsonMsg);
            log.info("消费REDIS消息队列数据成功：" + exJson);
        } catch (Exception e) {
            log.error("消费REDIS消息队列phone数据失败，失败信息:{}", e.getMessage());
        }
        latch.countDown();
    }
}
