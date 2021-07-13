package io.openvidu.server.job;

import io.openvidu.server.annotation.DistributedLock;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.manage.StatisticsManage;
import io.openvidu.server.common.pojo.ConfStatisticSearch;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.StatisticsConferenceDaily;
import io.openvidu.server.utils.DateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@EnableScheduling
public class StatisticsJobHandler {

    @Resource
    private CacheManage cacheManage;

    @Resource
    private StatisticsManage statisticsManage;


    /**
     * 每日会议统计任务
     * 01:00 执行(24h)
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @DistributedLock(key = "dayOfMeeting")
    public void dailyStatisticsJob() {
        long jobStart = System.currentTimeMillis();
        // calc query time
        Date queryStartTime = DateUtil.getDifferenceDate(Calendar.DATE, -1);
        Date queryEndTime = DateUtil.getDifferenceDate(Calendar.SECOND, -1);

        // query corporationInfo
        List<Corporation> corporations = statisticsManage.selectCorporations();
        if (CollectionUtils.isEmpty(corporations)) {
            log.info("No corporation found and close the job of statistic.");
            return;
        }

        corporations.forEach(corporation -> {
            log.info("statistics job project:{}, corpName:{}", corporation.getProject(), corporation.getCorpName());
            try {
                StatisticsConferenceDaily statisticsConferenceDaily = new StatisticsConferenceDaily();
                statisticsConferenceDaily.setStatisticTime(queryStartTime);
                statisticsConferenceDaily.setMaxConcurrent(cacheManage.getMaxConcurrentOfDayAndDel(corporation.getProject(), queryEndTime));
                statisticsConferenceDaily.setProject(corporation.getProject());
                //statisticsConferenceDaily.setAccessKey(application.getAccessKey());

                ConcurrentHashMap<String, DailyInfo> corpConfDailyMap = new ConcurrentHashMap<>();
                // query conference
                List<Conference> conferenceList = statisticsManage.queryConferenceByTimeRange(ConfStatisticSearch.builder()
                        .project(corporation.getProject()).startTime(queryStartTime).endTime(queryEndTime)
                        .status(ConferenceStatus.FINISHED.getStatus()).build());
                log.info("corporation:{} conference size:{}", corporation.getProject(), conferenceList.size());
                if (!CollectionUtils.isEmpty(conferenceList)) {
                    conferenceList.forEach(conference -> {
                        if (ConferenceStatus.FINISHED.getStatus() != conference.getStatus()) {
                            return;
                        }
                        corpConfDailyMap.computeIfPresent(conference.getProject(), (project, dailyInfo) -> {
                            dailyInfo.getRuids().add(conference.getRuid());
                            if (Objects.nonNull(conference.getStartTime()) && Objects.nonNull(conference.getEndTime())
                                    && conference.getEndTime().getTime() - conference.getStartTime().getTime() > 0) {
                                long duration = (conference.getEndTime().getTime() - conference.getStartTime().getTime()) / 60000;
                                dailyInfo.setDurationCount(dailyInfo.getDurationCount() + duration);
                            }
                            return dailyInfo;
                        });

                        corpConfDailyMap.computeIfAbsent(conference.getProject(), project -> {
                            DailyInfo dailyInfo = new DailyInfo();
                            dailyInfo.getRuids().add(conference.getRuid());
                            //dailyInfo.setAccessKey(conference.getAccessKey());
                            if (Objects.nonNull(conference.getStartTime()) && Objects.nonNull(conference.getEndTime())
                                    && conference.getEndTime().getTime() - conference.getStartTime().getTime() > 0) {
                                long duration = (conference.getEndTime().getTime() - conference.getStartTime().getTime()) / 60000;
                                dailyInfo.setDurationCount(dailyInfo.getDurationCount() + duration);
                            }
                            return dailyInfo;
                        });
                    });
                }

                if (!corpConfDailyMap.isEmpty()) {
                    corpConfDailyMap.forEach((k, v) -> {
                        statisticsConferenceDaily.setTotalDuration(v.getDurationCount());

                        // calc daily total participants of project
                        List<ConferencePartHistory> conferencePartHistoryList;
                        if (!CollectionUtils.isEmpty(v.getRuids()) && !CollectionUtils.isEmpty(conferencePartHistoryList =
                                statisticsManage.selectConfPartHistoryByRuids(new ArrayList<>(v.getRuids())))) {
                            ConcurrentHashMap<String, Set<String>> ruidPartUUidsMap = new ConcurrentHashMap<>();
                            conferencePartHistoryList.forEach(conferencePartHistory -> {
                                ruidPartUUidsMap.computeIfPresent(conferencePartHistory.getRuid(), (ruid, uuidSet) -> {
                                    uuidSet.add(conferencePartHistory.getUuid());
                                    return uuidSet;
                                });

                                ruidPartUUidsMap.computeIfAbsent(conferencePartHistory.getRuid(), ruid -> {
                                    Set<String> uuidSet = new HashSet<>();
                                    uuidSet.add(conferencePartHistory.getUuid());
                                    return uuidSet;
                                });
                            });

                            AtomicInteger totalParticipantSize = new AtomicInteger(0);
                            ruidPartUUidsMap.values().forEach(uuidSet -> totalParticipantSize.addAndGet(uuidSet.size()));

                            statisticsConferenceDaily.setConfCount(Long.valueOf(String.valueOf(v.getRuids().size())));
                            statisticsConferenceDaily.setTotalParticipants(Long.valueOf(String.valueOf(totalParticipantSize.get())));
                        }
                    });
                }

                statisticsManage.insert(statisticsConferenceDaily);
            } catch (Exception e) {
                log.info("Exception:", e);
            }
        });

        log.info("Daily:{} statistics job finished and cost:{} ms. ",
                DateUtil.getDateFormat(queryStartTime, "yyyyMMdd"), (System.currentTimeMillis() - jobStart));
        return;
    }

    @Getter
    @Setter
    private class DailyInfo {
        private Set<String> ruids = new HashSet<>();
        private Long durationCount = 0L;
    }
}
