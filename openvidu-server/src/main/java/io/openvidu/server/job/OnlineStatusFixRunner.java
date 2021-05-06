package io.openvidu.server.job;

import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.enums.DeviceStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


@Component
@Slf4j
public class OnlineStatusFixRunner implements ApplicationRunner {

    @Autowired
    private CacheManage cacheManage;

    @Resource(name = "tokenStringTemplate")
    private StringRedisTemplate tokenStringTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Set<String> keys = tokenStringTemplate.keys(CacheKeyConstants.DEV_PREFIX_KEY + "*");

        if (keys != null) {
            for (String key : keys) {
                String deviceNumber = key.replace(CacheKeyConstants.DEV_PREFIX_KEY, "");
                String status = cacheManage.getDeviceStatus(deviceNumber);
                if (!Objects.equals(DeviceStatus.offline.name(), status)) {
                    log.info("update {} status: offline", deviceNumber);
                    cacheManage.setDeviceStatus(deviceNumber, DeviceStatus.offline.name());
                }
            }
        }
        keys = tokenStringTemplate.keys(CacheKeyConstants.APP_TOKEN_PREFIX_KEY + "*");
        if (keys != null) {
            for (String key : keys) {
                String uuid = key.replace(CacheKeyConstants.APP_TOKEN_PREFIX_KEY, "");
                Map info = cacheManage.getUserInfoByUUID(uuid);
                if (info != null && info.containsKey("status")) {
                    String status = info.get("status").toString();
                    if (!Objects.equals(DeviceStatus.offline.name(), status)) {
                        log.info("update {} status: offline", uuid);
                        cacheManage.updateTokenInfo(uuid, "status", DeviceStatus.offline.name());
                    }
                }
            }
        }
    }
}
