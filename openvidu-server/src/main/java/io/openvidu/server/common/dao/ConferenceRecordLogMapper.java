package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConferenceRecordLog;

public interface ConferenceRecordLogMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ConferenceRecordLog record);

    int insertSelective(ConferenceRecordLog record);

    ConferenceRecordLog selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ConferenceRecordLog record);

    int updateByPrimaryKey(ConferenceRecordLog record);

}
