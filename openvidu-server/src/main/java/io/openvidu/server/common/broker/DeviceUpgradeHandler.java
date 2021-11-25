package io.openvidu.server.common.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.annotation.DistributedLock;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

/**
 * @author chosongi
 * @date 2019/12/25 18:13
 */

@Slf4j
@Component
public class DeviceUpgradeHandler {

    @Value("${device.upgrade.handler.thread.num}")
    private int threadNum;


    private static final Gson gson = new GsonBuilder().create();

    private static BlockingQueue<String> deviceInfos = new LinkedBlockingDeque<>(100);

    @Resource
    private RpcNotificationService rpcNotificationService;

    @PostConstruct
    public void init() {
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < threadNum; i++) {
            cachedThreadPool.execute(new UpgradeThread());
        }
    }





    @DistributedLock(key = "upgradeDevice")
     void notifyDevice2Upgrade(String devUpInfos) {
        JsonArray devInfos = gson.fromJson(devUpInfos, JsonArray.class);
        devInfos.forEach(devUpInfo -> {
            if (deviceInfos.offer(devUpInfo.toString())) {
                log.info("Upgrade info:{} is offering in the queue.", devUpInfo.toString());
            } else {
                log.error("NO EXTRA SPACE FOR OFFER DEVICE UPGRADE INFO.");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private class UpgradeThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    String devUpdateInfo = deviceInfos.take();
                    JsonObject devUpdObj = gson.fromJson(devUpdateInfo, JsonObject.class);
                    List<RpcConnection> rpcConnections = rpcNotificationService.getRpcConnections().stream().filter(rpcConn ->
                        !Objects.isNull(rpcConn.getSerialNumber()) && Objects.equals(AccessTypeEnum.terminal, rpcConn.getAccessType())
                        && devUpdObj.get("serialNumber").getAsString().equals(rpcConn.getSerialNumber())).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(rpcConnections)) {
                        RpcConnection rpcConnection = rpcConnections.stream()
                                .max(Comparator.comparing(RpcConnection::getCreateTime)).orElse(null);
                        if (!Objects.isNull(rpcConnection)) {
                            rpcNotificationService.sendNotification(rpcConnection.getParticipantPrivateId(),
                                    ProtocolElements.UPGRADE_NOTIFY_METHOD, devUpdObj);
                            log.info("Upgrade info:{} send to the dev by the rpc connection and private id:{}",
                                    devUpdateInfo, rpcConnection.getParticipantPrivateId());
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
