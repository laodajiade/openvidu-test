package io.openvidu.server.core;


import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * @author even
 * @date 2020/10/16 10:21
 */
@Slf4j
@Component
public class TimerManager {

    private static Timer timer = null;

    private static TimerTask timerTask = null;
    private int index = 0;


    public void onStartPolling(int autoSeconds,Session session, RpcNotificationService notificationService) {
        int interval = 10;
        int maxInterval = 60;
        if (autoSeconds < interval || autoSeconds > maxInterval) {
            autoSeconds = interval;
        }

        Date autoTime = new Date();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                dealPollingCheck(session, notificationService);
            }
        };
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, autoTime, autoSeconds * 1000);
    }



    public void dealPollingCheck(Session session, RpcNotificationService notificationService) {
        Set<Participant> pollingPartSet = session.getPartsExcludeModeratorAndSpeaker();
        if (!CollectionUtils.isEmpty(pollingPartSet)) {

            List<Participant> participants = pollingPartSet.stream().sorted(Comparator.comparing(Participant::getOrder)).collect(Collectors.toList());
            log.info("need to polling participants:{}", JSON.toJSONString(participants));
            if (index > participants.size() - 1) {
                index = 0;
            }
            Participant participant = participants.get(index);
            //notify current part polling to
            JsonObject currentNotifyParam = new JsonObject();
            currentNotifyParam.addProperty(ProtocolElements.POLLING_CONNECTIONID_METHOD, participant.getParticipantPublicId());
            session.getMajorPartEachIncludeThorConnect().forEach(part -> notificationService.sendNotification(part.getParticipantPrivateId(),
                    ProtocolElements.POLLING_TO_NOTIFY_METHOD, currentNotifyParam));

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


    public void onStopPolling() {
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (timer != null) {
            timer.cancel();
        }
    }

}
