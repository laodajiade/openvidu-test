package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.dao.StatisticsConferenceDailyMapper;
import io.openvidu.server.common.manage.StatisticsManage;
import io.openvidu.server.common.pojo.ConfStatisticSearch;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.StatisticsConferenceDaily;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chosongi
 * @date 2020/7/22 15:15
 */
@Slf4j
@Service
public class StatisticsManageImpl implements StatisticsManage {

    @Resource
    private StatisticsConferenceDailyMapper statisticsConferenceDailyMapper;

    @Resource
    private ConferenceMapper conferenceMapper;

    @Resource
    private ConferencePartHistoryMapper conferencePartHistoryMapper;

    @Resource
    private CorporationMapper corporationMapper;

    @Resource
    private CacheManage cacheManage;

    @Value("${duration.lessthan.tenhour}")
    private int durationLessThanTenHour;

    @Override
    public List<Conference> queryConferenceByTimeRange(ConfStatisticSearch confStatisticSearch) {
        return conferenceMapper.queryConferenceByTimeRange(confStatisticSearch);
    }

    @Override
    public List<ConferencePartHistory> selectConfPartHistoryByRuids(List<String> ruids) {
        return conferencePartHistoryMapper.selectConfPartHistoryByRuids(ruids);
    }

    @Override
    public void insert(StatisticsConferenceDaily statisticsConferenceDaily) {
        statisticsConferenceDailyMapper.insertSelective(statisticsConferenceDaily);
    }

    @Override
    public List<Corporation> selectCorporations() {
        return statisticsConferenceDailyMapper.selectCorporations();
    }

    @Override
    public Map<String, Integer> statisticsRemainderDuration(String project) {

        Map<String, Integer> map = new HashMap<>(2);
        int remainderDuration = cacheManage.getCorpRemainDuration(project);
        if (remainderDuration <= 0) {
            Corporation corporation = corporationMapper.selectByCorpProject(project);
            cacheManage.setCorpRemainDuration(project, corporation.getRemainderDuration());
            remainderDuration = corporation.getRemainderDuration();
        }
        if (remainderDuration > 0) {
            cacheManage.delCorpRemainDurationUsedUp(project);
        }
        if (remainderDuration >= durationLessThanTenHour) {
            cacheManage.delCorpRemainDurationLessTenHour(project);
        }
        map.put("remainderHour", remainderDuration / 60);
        map.put("remainderMinute", remainderDuration % 60);
        int count = conferencePartHistoryMapper.countProcessPartHistory(project);
        if (count >= 0) {
            int totalUsedDuration = count;
            int remainderTotalDuration = remainderDuration - totalUsedDuration;
            log.info("企业:{},在会议中与会者耗时-totalUsedDuration:{},剩余时长-remainderDuration:{}", project, totalUsedDuration, remainderTotalDuration);
            int remainderHour = remainderTotalDuration / 60;
            int remainderMinute = remainderTotalDuration % 60;
            cacheManage.setCorpRemainDuration(project, remainderTotalDuration);
            map.put("remainderHour", remainderHour);
            map.put("remainderMinute", remainderMinute);
        } else {
            Corporation corporation = corporationMapper.selectByCorpProject(project);
            cacheManage.setCorpRemainDuration(project, corporation.getRemainderDuration());
        }
        return map;
    }
}
