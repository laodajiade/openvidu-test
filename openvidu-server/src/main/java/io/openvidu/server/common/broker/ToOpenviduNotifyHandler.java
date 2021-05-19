package io.openvidu.server.common.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.rpc.handlers.UrgedPeopleToEndHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author chosongi
 * @date 2020/7/10 11:01
 */

@Slf4j
@Component
public class ToOpenviduNotifyHandler {

    private static final Gson gson = new GsonBuilder().create();

    @Resource
    private RpcNotificationService rpcNotificationService;

    @Autowired
    private UrgedPeopleToEndHandler urgedPeopleToEndHandler;

    @Resource
    private CacheManage cacheManage;

    @Value("${device.upload.url}")
    private String devUploadUrl;

    void notify(String message) {
        try {
            JsonObject obj = gson.fromJson(message, JsonObject.class);
            String method = obj.get("method").getAsString();
            JsonObject params = obj.getAsJsonObject("params");

            switch (method) {
                case ProtocolElements.URGED_PEOPLE_TO_END_METHOD:
                    urgedPeopleToEndHandler.notifyToModerator(params);
                    break;
            }

        } catch (Exception e) {
            log.error("ToOpenviduNotifyHandler error", e);
        }


    }
}
