package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConferenceRecord;

import java.util.List;

public interface ConferenceRecordMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ConferenceRecord record);

    int insertSelective(ConferenceRecord record);

    ConferenceRecord selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ConferenceRecord record);

    int updateByRuidSelective(ConferenceRecord update);

    int updateByPrimaryKey(ConferenceRecord record);

    List<ConferenceRecord> getByCondition(ConferenceRecord record);

    void decreaseConferenceRecordCountByRuid(String ruid);

    void deleteUselessRecord();

    int updateByRuid(ConferenceRecord update);

    void increaseConferenceRecordCountByRuid(String ruid);

}
