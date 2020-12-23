package io.openvidu.server.job;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.constants.CommonConstants;
import io.openvidu.server.common.dao.ConferenceRecordInfoMapper;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.manage.ConferenceRecordManage;
import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 录制服务到期任务
 */
@Component
@Slf4j
public class RecordServiceSchedule {

    @Resource
    protected ConferenceRecordManage conferenceRecordManage;

    @Resource
    protected ConferenceRecordInfoMapper conferenceRecordInfoMapper;


    private static final Integer freeUpByExpiredDay = 15;

    @Resource
    private CorporationMapper corporationMapper;

    @Resource
    private SessionManager sessionManager;

    @Resource
    protected RpcNotificationService notificationService;

    /**
     * 到期一天，停止所有的录制服务
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void stopRecord() {
        List<Corporation> corporations = corporationMapper.listByCorpRecordExpireDay(DateFormatUtils.format(DateUtils.addDays(new Date(), -1), "yyyy-MM-dd"));
        for (Corporation corporation : corporations) {
            stopRecord(corporation.getProject());
        }
    }

    public void stopRecord(String project) {
        Collection<Session> sessions = sessionManager.getCorpSessions(project);

        if (CollectionUtils.isEmpty(sessions)) {
            return;
        }

        for (Session session : sessions) {
            stopRecord(session);
        }
    }

    public void stopRecord(Session session) {
        if (!session.sessionAllowedToStopRecording()) {
            return;
        }
        log.info("record expired to stop record roomId={}, begin...", session.getSessionId());
        sessionManager.setStopRecordingTime(session.getSessionId(), System.currentTimeMillis());

        // 通知录制服务停止录制视频
        sessionManager.stopRecording(session.getSessionId());

        // 通知与会者停止录制
        notifyStopRecording(session.getSessionId());
        log.info("record expired to stop record roomId={}, end...", session.getSessionId());
    }

    private void notifyStopRecording(String sessionId) {
        JsonObject notify = new JsonObject();
        notify.addProperty("reason", CommonConstants.RECORD_STOP_BY_MODERATOR);
        sessionManager.getSession(sessionId).getParticipants().forEach(participant ->
                this.notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.STOP_CONF_RECORD_METHOD, notify));
    }


    /**
     * 到期15天，释放空间
     */
    @Scheduled(cron = "0 0/1 * * * ?")
    public void freeUpRecordSpace() {
        String day = DateFormatUtils.format(DateUtils.addDays(new Date(), -freeUpByExpiredDay - 1), "yyyy-MM-dd");
        List<Corporation> corporations = corporationMapper.listByCorpRecordExpireDay(day);
        for (Corporation corporation : corporations) {
            freeUpRecordSpace(corporation.getProject());
        }
    }


    public void freeUpRecordSpace(String project) {
        log.info("freeUpRecordSpace by project: {}", project);
        List<ConferenceRecordInfo> infos = conferenceRecordInfoMapper.selectByProject(project);
        if (infos.isEmpty()) {
            return;
        }
        conferenceRecordManage.deleteConferenceRecord(infos);
    }

}
