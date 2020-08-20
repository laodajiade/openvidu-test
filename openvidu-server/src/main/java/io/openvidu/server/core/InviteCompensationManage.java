package io.openvidu.server.core;

import com.google.gson.JsonElement;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * @author chosongi
 * @date 2020/3/26 20:17
 */
@Slf4j
@Component
public class InviteCompensationManage {

    @Resource
    private RpcNotificationService notificationService;

    @Resource(name = "reconnectCompensationScheduler")
    private TaskScheduler taskScheduler;

    private static ConcurrentHashMap<String, InviteCompensationScheduler> map = new ConcurrentHashMap<>();

    public void activateInviteCompensation(String account, JsonElement jsonElement, Long expireTime) {
        map.computeIfAbsent(account, accountKey -> {
            log.info("Activate Invite Compensation account:{}, expireTime:{}", account, expireTime);
            InviteCompensationScheduler scheduler = new InviteCompensationScheduler(account, jsonElement, expireTime);
            scheduler.activateSendTask();
            return scheduler;
        });
    }

    public void disableInviteCompensation(String account) {
        InviteCompensationScheduler scheduler = map.remove(account);
        if (!Objects.isNull(scheduler)) {
            log.info("Disable Invite Compensation account:{}", account);
            scheduler.disable();
        }
    }

    public class InviteCompensationScheduler {

        @NotNull
        private String account;

        @NotNull
        private JsonElement jsonElement;

        @NotNull
        private Long expireTime;

        InviteCompensationScheduler(String account, JsonElement jsonElement, Long expireTime) {
            this.account = account;
            this.jsonElement = jsonElement;
            this.expireTime = expireTime;
        }

        private ScheduledFuture<?> sendInviteTask;

        private Runnable sendInviteNotify = new Runnable() {
            @Override
            public void run() {
                notificationService.getRpcConnections().stream()
                        .filter(rpcConnection -> Objects.equals(rpcConnection.getUserUuid(), account)
                                && Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.terminal))
                        .max(Comparator.comparing(RpcConnection::getCreateTime))
                        .ifPresent(rpcConnect ->
                                notificationService.sendNotification(rpcConnect.getParticipantPrivateId(),
                                        ProtocolElements.INVITE_PARTICIPANT_METHOD, jsonElement));
            }
        };

        private ScheduledFuture<?> countDownTask;

        private Runnable taskCountDown = this::disableSendTask;

        private void disableSendTask() {
            if (sendInviteTask != null) {
                sendInviteTask.cancel(false);
                log.info("Disable Invite Compensation by CountDown Task account:{}", account);
            }
        }

        void activateSendTask() {
            sendInviteTask = taskScheduler.scheduleAtFixedRate(sendInviteNotify, new Date(System.currentTimeMillis() + 3000), 3000);
            countDownTask = taskScheduler.schedule(taskCountDown, new Date(expireTime));
        }

        void disable() {
            if (countDownTask != null) {
                countDownTask.cancel(false);
            }

            if (sendInviteTask != null) {
                sendInviteTask.cancel(false);
            }
        }

    }
}
