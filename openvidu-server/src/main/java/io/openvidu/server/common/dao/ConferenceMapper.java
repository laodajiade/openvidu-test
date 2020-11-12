package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConfStatisticSearch;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.common.pojo.dto.CorpRoomsSearch;

import java.util.List;

public interface ConferenceMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Conference record);

    int insertSelective(Conference record);

    Conference selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Conference record);

    int updateByPrimaryKey(Conference record);

    List<Conference> selectBySearchCondition(ConferenceSearch search);

    List<Conference> selectBySearchParam(CorpRoomsSearch search);

    List<Conference> selectUnclosedConference(Conference conference);

    Conference selectByRuid(String ruid);

    void deleteByRuid(String ruid);

    List<Conference> queryConferenceByTimeRange(ConfStatisticSearch confStatisticSearch);

    List<Conference> selectPageRecordsByCondition(ConferenceSearch search);

    long selectConfRecordsCountByCondition(ConferenceSearch search);

    List<Conference> getFinishedList(Long userId);

    void softDeleteById(Long id);

    void changeRealStartTime(String ruid);
}
