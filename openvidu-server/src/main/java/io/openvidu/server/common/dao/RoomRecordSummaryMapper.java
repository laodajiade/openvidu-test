package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import io.openvidu.server.common.pojo.RoomRecordSummary;

import java.util.List;

public interface RoomRecordSummaryMapper {
    int deleteByPrimaryKey(Long id);

    int insert(RoomRecordSummary record);

    int insertSelective(RoomRecordSummary record);

    RoomRecordSummary selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(RoomRecordSummary record);

    int updateByPrimaryKey(RoomRecordSummary record);

    List<RoomRecordSummary> selectByCondition(ConferenceRecordSearch search);

    void deleteByRoomId(String roomId);

    void decreaseRecordSummary(RoomRecordSummary update);

    void deleteUselessSummaryInfo();
}