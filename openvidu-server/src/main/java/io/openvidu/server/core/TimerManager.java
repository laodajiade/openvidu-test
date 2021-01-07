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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    public void startPollingCompensation(String roomId, int intervalTime, int index) {
        map.computeIfAbsent(roomId, accountKey -> {
            PollingCompensationScheduler scheduler = new PollingCompensationScheduler(roomId, intervalTime, index);
            scheduler.startPollingTask();
            log.info("start polling in room:{},intervalTime:{}", roomId, intervalTime);
            return scheduler;
        });
    }

    public void leaveRoomStartPollingAgainCompensation(String roomId, int intervalTime, int index) {
        log.info("leaveRoomStartPollingAgainCompensation roomId:{} intervalTime:{} index:{}", roomId, intervalTime, index);
        PollingCompensationScheduler pollingCompensationScheduler = map.remove(roomId);
        if (!Objects.isNull(pollingCompensationScheduler)) {
            log.info("leaveRoom stop polling Task roomId:{}", roomId);
            pollingCompensationScheduler.leaveRoomDisable();
        }
        map.computeIfAbsent(roomId, accountKey -> {
            PollingCompensationScheduler scheduler = new PollingCompensationScheduler(roomId, intervalTime, index);
            scheduler.setFirst(1);
            scheduler.startPollingTask();
            log.info("leaveRoom start polling in room:{},intervalTime:{}", roomId, intervalTime);
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

    public Map<String,Integer> getPollingCompensationScheduler(String roomId) {
        PollingCompensationScheduler scheduler = map.get(roomId);
        Map<String,Integer> schedulerMap = new HashMap<>(2);
        schedulerMap.put("index",scheduler.getIndex());
        schedulerMap.put("order",scheduler.getOrder());
        return schedulerMap;
    }


    class PollingCompensationScheduler{
        private int index;
        private int first = 0;
        private String roomId;
        private int intervalTime;
        private int order;

        public int getFirst() {
            return first;
        }

        public void setFirst(int first) {
            this.first = first;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getIntervalTime(int intervalTime){
            if (intervalTime < 0 || intervalTime > 60) {
                intervalTime = 5;
            }
            return intervalTime;
        }

        public PollingCompensationScheduler(String roomId, int intervalTime, int index){
            this.roomId = roomId;
            this.intervalTime = intervalTime;
            this.index = index;
        }

        private ScheduledFuture<?> pollingTask;

        private Runnable pollingNotify = new Runnable() {
            @Override
            public void run() {
                dealPollingCheck(sessionManager.getSession(roomId),notificationService);
            }
        };

        public void dealPollingCheck(Session session, RpcNotificationService notificationService) {
            if (Objects.isNull(session)) {
                log.info("start polling undo when session is null");
                return;
            }
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
                    currentNotifyParam.addProperty(ProtocolElements.POLLING_CONNECTIONID_PARAM, participant.getParticipantPublicId());
                    session.getMajorPartEachIncludeThorConnect().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                            ProtocolElements.POLLING_TO_NOTIFY_METHOD, currentNotifyParam));
                    first++;
                }
                //send notify polling check
                JsonObject jsonCheckParam = new JsonObject();
                jsonCheckParam.addProperty(ProtocolElements.POLLING_CONNECTIONID_PARAM, participant.getParticipantPublicId());
                if (participant.isStreaming()) {
                    setOrder(participant.getOrder());
                    jsonCheckParam.addProperty(ProtocolElements.POLLING_ISCHECK_PARAM, true);
                } else {
                    jsonCheckParam.addProperty(ProtocolElements.POLLING_ISCHECK_PARAM, false);
                }

                log.info("roomId:{} polling check part:{} the index:{}", session.getSessionId(), participant.getParticipantPublicId(), index);
                session.getMajorPartEachIncludeThorConnect().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                        ProtocolElements.POLLING_CHECK_NOTIFY_METHOD, jsonCheckParam));

                //notify next part polling to
                int notifyIndex = index + 1;
                if (notifyIndex > participants.size() - 1) {
                    notifyIndex = 0;
                }
                log.info("roomId:{} advance notify next part:{} polling to the index:{}", session.getSessionId(), participants.get(notifyIndex).getParticipantPublicId(), notifyIndex);
                JsonObject nextNotifyParam = new JsonObject();
                nextNotifyParam.addProperty(ProtocolElements.POLLING_CONNECTIONID_PARAM, participants.get(notifyIndex).getParticipantPublicId());
                session.getMajorPartEachIncludeThorConnect().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                        ProtocolElements.POLLING_TO_NOTIFY_METHOD, nextNotifyParam));
                index++;
            }
        }

        void startPollingTask() {
            pollingTask = taskScheduler.scheduleAtFixedRate(pollingNotify, new Date(), getIntervalTime(intervalTime) * 1000);
        }

        void disable() {
            if (pollingTask != null) {
                pollingTask.cancel(false);
                first = 0;
            }
        }

        void leaveRoomDisable() {
            if (pollingTask != null) {
                pollingTask.cancel(false);
            }
        }
    }

}
