package io.openvidu.server.common.broker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2020/7/10 11:01
 */

@Slf4j
@Component
public class NotifyDeviceUploadLogHandler {

    private static final Gson gson = new GsonBuilder().create();

    @Resource
    private RpcNotificationService rpcNotificationService;

    @Resource
    private CacheManage cacheManage;

    @Value("${device.upload.url}")
    private String devUploadUrl;

    void notifyDev2UploadLog(String message) {
        JsonObject accountObj = gson.fromJson(message, JsonObject.class);
        String uuid;
        if (accountObj.has("uuid") && !StringUtils.isEmpty(uuid = accountObj.get("uuid").getAsString())) {
            rpcNotificationService.getRpcConnections()
                    .stream()
                    .filter(rpcConnect -> Objects.equals(uuid, rpcConnect.getUserUuid()))
                    .max(Comparator.comparing(RpcConnection::getCreateTime))
                    .ifPresent(rpcConn -> {
                        // save upload token
                        String uploadToken;
                        cacheManage.setLogUploadToken(uuid, uploadToken = StringUtil.getNonce(32));

                        // construct notify parameters
                        JsonObject notifyParams = new JsonObject();
                        notifyParams.addProperty("uploadToken", uploadToken);
                        notifyParams.addProperty("uploadUrl", devUploadUrl);

                        // notify dev to upload log
                        rpcNotificationService.sendNotification(rpcConn.getParticipantPrivateId(), ProtocolElements.UPLOADDEVICELOG_NOTIFY, notifyParams);
                    });
        }
    }
}
