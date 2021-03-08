package io.openvidu.server.common.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author even
 * @date 2021/3/8 14:45
 */

@Slf4j
@Component
public class NotifyDeviceInfoUpdateHandler {

    private static final Gson gson = new GsonBuilder().create();

    private static BlockingQueue<String> deviceInfos = new LinkedBlockingDeque<>(100);

    @Resource
    private RpcNotificationService rpcNotificationService;

    @PostConstruct
    public void init() {
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < 1; i++) {
            cachedThreadPool.execute(new UpdateThread());
        }
    }


    static void notifyDeviceInfoUpdate(String devUpInfos) {
        log.info("device update info:{} is offering in the queue.", devUpInfos);
        deviceInfos.offer(devUpInfos);
    }

    private class UpdateThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String devUpdateInfo = deviceInfos.take();
                    JsonObject devUpdObj = gson.fromJson(devUpdateInfo, JsonObject.class);
                    RpcConnection rpcConnection = rpcNotificationService.getRpcConnections().stream().filter(rpcConn ->
                            !Objects.isNull(rpcConn.getSerialNumber()) && Objects.equals(AccessTypeEnum.terminal, rpcConn.getAccessType())
                                    && devUpdObj.get("serialNumber").getAsString().equals(rpcConn.getSerialNumber()))
                            .max(Comparator.comparing(RpcConnection::getCreateTime))
                            .orElse(null);

                    if (!Objects.isNull(rpcConnection)) {
                        rpcNotificationService.sendNotification(rpcConnection.getParticipantPrivateId(),
                                ProtocolElements.DEVICE_INFO_UPDATE_NOTIFY_METHOD, devUpdObj);
                        log.info("device update info:{} send to the dev by the rpc connection and private id:{}",
                                devUpdateInfo, rpcConnection.getParticipantPrivateId());
                    }
                } catch (InterruptedException e) {
                    log.error("deviceInfo update error", e);
                }
            }
        }
    }

}
