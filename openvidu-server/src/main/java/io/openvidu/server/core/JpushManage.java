package io.openvidu.server.core;

import cn.jiguang.common.resp.APIConnectionException;
import cn.jiguang.common.resp.APIRequestException;
import cn.jpush.api.JPushClient;
import cn.jpush.api.push.model.notification.IosAlert;
import io.openvidu.server.common.dao.JpushMessageMapper;
import io.openvidu.server.common.pojo.JpushMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

/**
 * @author even
 * @date 2021/1/13 11:30
 */
@Slf4j
@Component
public class JpushManage {

    @Value("${jpush.app.key}")
    private String appKey;
    @Value("${jpush.app.secret}")
    private String appSecret;

    private JPushClient jPushClient;

    @Resource
    private JpushMessageMapper jpushMessageMapper;

    @PostConstruct
    public void initJpushClient() {
        if (null == jPushClient) {
            synchronized (JPushClient.class) {
                jPushClient = new JPushClient(appKey, appSecret);
            }
        }
    }

    public void sendToAndroid(String title, String alert, Map<String,String> map, String ... registrationId) {
        try {
            jPushClient.sendAndroidNotificationWithRegistrationID(title, alert, map, registrationId);
        } catch (APIConnectionException | APIRequestException e) {
            log.error("极光消息推送Android异常：", e);
        }
    }

    public void sendToIos(IosAlert alert, Map<String, String> extras, String... registrationId) {
        try {
            jPushClient.sendIosNotificationWithRegistrationID(alert, extras, registrationId);
        } catch (APIConnectionException | APIRequestException e) {
            log.error("极光消息推送iOS异常：", e);
        }
    }
}
