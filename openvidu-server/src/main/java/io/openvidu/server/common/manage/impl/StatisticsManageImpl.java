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
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2020/7/22 15:15
 */
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
        Corporation corporation = corporationMapper.selectByCorpProject(project);
        Map<String,Integer> map = new HashMap<>(2);
        map.put("remainderHour",corporation.getRemainderDuration()/60);
        map.put("remainderMinute",corporation.getRemainderDuration()%60);
        cacheManage.setCorpRemainDuration(project,corporation.getRemainderDuration());
        if (Objects.nonNull(corporation)) {
            int remainderDuration = corporation.getRemainderDuration();
            if (Objects.nonNull(remainderDuration)) {
                List<ConferencePartHistory> conferencePartHistories = conferencePartHistoryMapper.selectProcessPartHistory(project);
                if (!CollectionUtils.isEmpty(conferencePartHistories)) {
                    int totalUsedDuration = 0;
                    Date currentDate = new Date();
                    for (ConferencePartHistory conferencePartHistory : conferencePartHistories) {
                        int usedDuration = (int) (currentDate.getTime() - conferencePartHistory.getStartTime().getTime())/6000;
                        totalUsedDuration += usedDuration;
                    }
                    int remainderTotalDuration = remainderDuration - totalUsedDuration;
                    int remainderHour = remainderTotalDuration/60;
                    int remainderMinute = remainderTotalDuration%60;
                    cacheManage.setCorpRemainDuration(project,remainderTotalDuration);
                    map.put("remainderHour",remainderHour);
                    map.put("remainderMinute",remainderMinute);
                }
            }
        }
        return map;
    }
}
