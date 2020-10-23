package io.openvidu.server.core;


import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * @author even
 * @date 2020/10/16 10:21
 */
@Slf4j
@Component
public class TimerManager {

    @Resource
    private SessionManager sessionManager;
    @Resource
    private RpcNotificationService notificationService;
    @Resource(name = "pollingCompensationScheduler")
    private TaskScheduler taskScheduler;

    private static ConcurrentHashMap<String, PollingCompensationScheduler> map = new ConcurrentHashMap<>();


    public void startPollingCompensation(String roomId, int intervalTime) {
        map.computeIfAbsent(roomId, accountKey -> {
            PollingCompensationScheduler scheduler = new PollingCompensationScheduler(roomId, intervalTime);
            scheduler.startPollingTask();
            log.info("start polling in room:{},intervalTime:{}", roomId, intervalTime);
            return scheduler;
        });
    }

    public void stopPollingCompensation(String roomId) {
        PollingCompensationScheduler scheduler = map.remove(roomId);
        if (!Objects.isNull(scheduler)) {
            log.info("stop polling Task roomId:{}", roomId);
            scheduler.disable();
        }
    }


    class PollingCompensationScheduler{
        private int index = 0;
        private int first = 0;
        private String roomId;
        private int intervalTime;

        public int getIntervalTime(int intervalTime){
            if (intervalTime < 10 || intervalTime > 60) {
                intervalTime = 10;
            }
            return intervalTime;
        }

        public PollingCompensationScheduler(String roomId, int intervalTime){
            this.roomId = roomId;
            this.intervalTime = intervalTime;
        }

        private ScheduledFuture<?> pollingTask;

        private Runnable pollingNotify = new Runnable() {
            @Override
            public void run() {
                dealPollingCheck(sessionManager.getSession(roomId),notificationService);
            }
        };

        public void dealPollingCheck(Session session, RpcNotificationService notificationService) {
            Set<Participant> pollingPartSet = session.getPartsExcludeModeratorAndSpeaker();
            if (!CollectionUtils.isEmpty(pollingPartSet)) {

                List<Participant> participants = pollingPartSet.stream().sorted(Comparator.comparing(Participant::getOrder)).collect(Collectors.toList());
                if (index > participants.size() - 1) {
                    index = 0;
                }
                Participant participant = participants.get(index);
                //notify current part:index=0 polling to
                if (first == 0) {
                    JsonObject currentNotifyParam = new JsonObject();
                    currentNotifyParam.addProperty(ProtocolElements.POLLING_CONNECTIONID_METHOD, participant.getParticipantPublicId());
                    session.getMajorPartEachIncludeThorConnect().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                            ProtocolElements.POLLING_TO_NOTIFY_METHOD, currentNotifyParam));
                    first++;
                }

                if (participant.isStreaming()) {
                    log.info("polling check part:{} the index:{}", participant.getParticipantPublicId(), index);
                    //send notify polling check
                    JsonObject jsonCheckParam = new JsonObject();
                    jsonCheckParam.addProperty(ProtocolElements.POLLING_CONNECTIONID_METHOD, participant.getParticipantPublicId());
                    session.getMajorPartEachIncludeThorConnect().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                            ProtocolElements.POLLING_CHECK_NOTIFY_METHOD, jsonCheckParam));

                    int nextToIndex = index + 1;
                    if (nextToIndex > participants.size() - 1) {
                        nextToIndex = 0;
                    }
                    //notify next part polling to
                    JsonObject nextNotifyParam = new JsonObject();
                    nextNotifyParam.addProperty(ProtocolElements.POLLING_CONNECTIONID_METHOD, participants.get(nextToIndex).getParticipantPublicId());
                    session.getMajorPartEachIncludeThorConnect().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                            ProtocolElements.POLLING_TO_NOTIFY_METHOD, nextNotifyParam));
                    index++;
                } else {
                    //notify next part polling to
                    int notifyIndex = index + 1;
                    if (notifyIndex > participants.size() - 1) {
                        notifyIndex = 0;
                    }
                    log.info("polling skip to next part:{}  the index:{}",participants.get(notifyIndex).getParticipantPublicId(), notifyIndex);
                    JsonObject nextNotifyParam = new JsonObject();
                    nextNotifyParam.addProperty(ProtocolElements.POLLING_CONNECTIONID_METHOD, participants.get(notifyIndex).getParticipantPublicId());
                    session.getMajorPartEachIncludeThorConnect().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                            ProtocolElements.POLLING_TO_NOTIFY_METHOD, nextNotifyParam));
                    index++;
                    dealPollingCheck(session,notificationService);

                }
            }
        }

        void startPollingTask() {
            pollingTask = taskScheduler.scheduleAtFixedRate(pollingNotify, new Date(), getIntervalTime(intervalTime) * 1000);
        }

        void disable() {
            if (pollingTask != null) {
                pollingTask.cancel(false);
                index = 0;
                first = 0;
            }
        }
    }

}
