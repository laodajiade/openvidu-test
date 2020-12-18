package io.openvidu.server.job;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.broker.CorpServiceExpiredNotifyHandler;
import io.openvidu.server.common.broker.RedisPublisher;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.constants.BrokerChannelConstans;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ParticipantStatusEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.manage.StatisticsManage;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.NotEndPartHistory;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.rpc.RpcNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author even
 * @date 2020/11/10 09:27
 */
@Slf4j
@Component
public class CorpRemainderDurationSchedule {

    @Resource
    private SessionManager sessionManager;

    @Resource
    private ConferencePartHistoryMapper conferencePartHistoryMapper;
    @Resource
    private CorporationMapper corporationMapper;
    @Resource
    private StatisticsManage statisticsManage;
    @Autowired
    CorpServiceExpiredNotifyHandler corpServiceExpiredNotifyHandler;
    @Resource
    CacheManage cacheManage;
    @Autowired
    private UserManage userManage;
    @Resource
    private RedisPublisher redisPublisher;
    @Autowired
    protected RpcNotificationService notificationService;

    @Value("${duration.lessthan.tenhour}")
    private int durationLessThanTenHour;

    @Scheduled(cron = "0 0/1 * * * ?")
    public void fixEndPartHistory(){
        List<NotEndPartHistory> notEndPartHistoryList = conferencePartHistoryMapper.selectNotEndPartHistory();
        if (!CollectionUtils.isEmpty(notEndPartHistoryList)) {
            notEndPartHistoryList.forEach(e -> {
                Session session = sessionManager.getSession(e.getRoomId());
                if (Objects.isNull(session) || session.isClosed()) {
                    ConferencePartHistory search = new ConferencePartHistory();
                    search.setRuid(e.getRuid());
                    List<ConferencePartHistory> conferencePartHistories = conferencePartHistoryMapper.selectByCondition(search);
                    if (!CollectionUtils.isEmpty(conferencePartHistories)) {
                        AtomicInteger totalDuration = new AtomicInteger();
                        conferencePartHistories.forEach(conferencePartHistory ->{
                            conferencePartHistory.setStatus(ParticipantStatusEnum.LEAVE.getStatus());
                            Date endTime = new Date();
                            conferencePartHistory.setEndTime(endTime);
                            int duration = (int) ((endTime.getTime() - conferencePartHistory.getStartTime().getTime()) / 60000);
                            conferencePartHistory.setDuration(duration == 0 ? 1 : duration);
                            totalDuration.addAndGet(duration);
                        });
                        conferencePartHistoryMapper.updateNotEndPartHistroy(conferencePartHistories);
                        Corporation corporation = corporationMapper.selectByCorpProject(e.getProject());
                        int remainderTotalDuration = corporation.getRemainderDuration() - totalDuration.get();
                        corporation.setRemainderDuration(remainderTotalDuration);
                        corporationMapper.updateCorpRemainderDuration(corporation);
                    }

                }
            });
        }
    }


    @Scheduled(cron = "0 0/1 * * * ?")
    public void countCorpRemainderDuration(){
        List<Corporation> corporations = corporationMapper.selectAllCorp();
        if (!CollectionUtils.isEmpty(corporations)) {
            corporations.forEach(corporation -> {
                if (Objects.nonNull(corporation.getRemainderDuration())) {
                    Map<String,Integer> map = statisticsManage.statisticsRemainderDuration(corporation.getProject());
                    int remainderDuration = cacheManage.getCorpRemainDuration(corporation.getProject());
                    log.info("当前企业：{} 剩余时长：{}", corporation.getProject(), remainderDuration);
                    if (remainderDuration > 0) {
                        cacheManage.delCorpRemainDurationUsedUp(corporation.getProject());
                    }
                    if (remainderDuration >= durationLessThanTenHour) {
                        cacheManage.delCorpRemainDurationLessTenHour(corporation.getProject());
                    }
                    if (remainderDuration > 0 && remainderDuration < durationLessThanTenHour ) {
                        String durationLessTenHour = cacheManage.getCorpRemainDurationLessTenHour(corporation.getProject());
                        if (org.apache.commons.lang.StringUtils.isEmpty(durationLessTenHour)) {
                            //获取企业管理员信息
                            User adminUser = userManage.getAdminUserByProject(corporation.getProject());
                            if (Objects.nonNull(adminUser) && !StringUtils.isEmpty(adminUser.getPhone())) {
                                JsonObject smsObj = new JsonObject();
                                JsonObject contentObj = new JsonObject();
                                contentObj.addProperty("project", corporation.getProject());

                                smsObj.addProperty("phoneNumber", adminUser.getPhone());
                                smsObj.add("content", contentObj);
                                smsObj.addProperty("smsType", "RemainderDuration");
                                redisPublisher.sendChnMsg(BrokerChannelConstans.SMS_DELIVERY_CHANNEL, smsObj.toString());
                            } else {
                                log.info("企业时长不足十小时,通知企业管理员uuid:{}, phone:{}",adminUser.getUuid() ,adminUser.getPhone());
                            }
                            //时长不足十小时通知
                            corpServiceExpiredNotifyHandler.notify(String.valueOf(corporation.getId()));
                        }
                        cacheManage.setCorpRemainDurationLessTenHour(corporation.getProject());
                    }
                    if (remainderDuration <= 0) {
                        String durationUsedUp = cacheManage.getCorpRemainDurationUsedUp(corporation.getProject());
                        if (org.apache.commons.lang.StringUtils.isEmpty(durationUsedUp)) {
                            corpServiceExpiredNotifyHandler.notify(String.valueOf(corporation.getId()));
                            Collection<Session> sessions = sessionManager.getCorpSessions(corporation.getProject());
                            if (!CollectionUtils.isEmpty(sessions)) {
                                JsonObject params = new JsonObject();
                                params.addProperty("reason", "callDurationUsedUp");
                                for (Session session : sessions) {
                                    Set<Participant> participants = sessionManager.getMajorParticipants(session.getSessionId());
                                    participants.forEach(p -> {
                                        if (p.getProject().equals(session.getConference().getProject())) {
                                            notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, params);
                                        } else {
                                            notificationService.sendNotification(p.getParticipantPrivateId(), ProtocolElements.CLOSE_ROOM_NOTIFY_METHOD, new JsonObject());
                                        }

                                        RpcConnection rpcConnect = notificationService.getRpcConnection(p.getParticipantPrivateId());
                                        if (!Objects.isNull(rpcConnect) && !Objects.isNull(rpcConnect.getSerialNumber())) {
                                            cacheManage.setDeviceStatus(rpcConnect.getSerialNumber(), DeviceStatus.online.name());
                                        }
                                    });
                                    sessionManager.stopRecording(session.getSessionId());
                                    sessionManager.closeSession(session.getSessionId(), EndReason.callDurationUsedUp);
                                }
                            }
                            cacheManage.setCorpRemainDurationUsedUp(corporation.getProject());
                        }
                    }
                }
            });
        }
    }
}
