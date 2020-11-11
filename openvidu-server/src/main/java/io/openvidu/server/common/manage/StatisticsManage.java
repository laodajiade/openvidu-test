package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.ConfStatisticSearch;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.StatisticsConferenceDaily;

import java.util.List;
import java.util.Map;

/**
 * @author chosongi
 * @date 2020/7/22 15:15
 */
public interface StatisticsManage {
    List<Conference> queryConferenceByTimeRange(ConfStatisticSearch confStatisticSearch);

    List<ConferencePartHistory> selectConfPartHistoryByRuids(List<String> ruids);

    void insert(StatisticsConferenceDaily statisticsConferenceDaily);

    List<Corporation> selectCorporations();

    Map<String,Integer> statisticsRemainderDuration(String project);
}
