package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.StatisticsConferenceDaily;

import java.util.List;

public interface StatisticsConferenceDailyMapper {
    int deleteByPrimaryKey(Long id);

    int insert(StatisticsConferenceDaily record);

    int insertSelective(StatisticsConferenceDaily record);

    StatisticsConferenceDaily selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(StatisticsConferenceDaily record);

    int updateByPrimaryKey(StatisticsConferenceDaily record);

    List<Corporation> selectCorporations();
}
