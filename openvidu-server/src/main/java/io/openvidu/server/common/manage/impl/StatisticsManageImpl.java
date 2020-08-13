package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.dao.StatisticsConferenceDailyMapper;
import io.openvidu.server.common.manage.StatisticsManage;
import io.openvidu.server.common.pojo.ConfStatisticSearch;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.StatisticsConferenceDaily;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

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
}
