package io.openvidu.server.core;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.annotation.DistributedLock;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author chosongi
 * @date 2020/3/26 20:17
 */
@Slf4j
@Component
public class InviteCompensationManage {

    @Resource
    private RpcNotificationService notificationService;

    @Resource
    private CacheManage cacheManage;


    /**
     * 每3秒发一次邀请
     */
    @Scheduled(cron = "0/3 * * * * ?")
    @DistributedLock(key = "inviteSchedule")
    public void inviteSchedule() {
        Map allInviteQueue = cacheManage.getAllInviteQueue();
        allInviteQueue.forEach((uuid, params) -> {
            try {
                JsonObject json = new GsonBuilder().create().fromJson((String) params, JsonObject.class);
                long expiredTime = json.get(ProtocolElements.INVITE_PARTICIPANT_EXPIRETIME_PARAM).getAsLong();
                if (System.currentTimeMillis() > expiredTime) {
                    cacheManage.delInviteQueue((String) uuid);
                    return;
                }

                notificationService.getRpcConnectionByUuids((String) uuid).forEach(rpcConnect ->
                        notificationService.sendNotification(rpcConnect.getParticipantPrivateId(), ProtocolElements.INVITE_PARTICIPANT_METHOD, json));
            } catch (Exception e) {
                cacheManage.delInviteQueue((String) uuid);
            }

        });
    }

    public void activateInviteCompensation(String account, JsonElement jsonElement, Long expireTime) {
        cacheManage.addInviteQueue(account, jsonElement.toString());
    }

    public void disableInviteCompensation(String account) {
        cacheManage.delInviteQueue(account);
    }

    public void disableAllInviteCompensation(String roomId) {
        Map inviteInfo = cacheManage.getInviteInfo(roomId);
        if (!inviteInfo.isEmpty()) {
            for (Object key : inviteInfo.keySet()) {
                String account = (String) key;
                cacheManage.delInviteQueue(account);

                notificationService.getRpcConnectionByUuids(account).forEach(rpcConnection -> {
                    log.info("Close Room CANCEL_INVITE_NOTIFY_METHOD account:{}", account);
                    notificationService.sendNotification(rpcConnection.getParticipantPrivateId(),
                            ProtocolElements.CANCELINVITE_NOTIFY_METHOD, new JsonObject());
                });
            }
        }
    }

}
