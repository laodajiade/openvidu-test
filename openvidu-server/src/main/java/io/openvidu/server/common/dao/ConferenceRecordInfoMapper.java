package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.common.pojo.ConferenceRecordSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConferenceRecordInfoMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ConferenceRecordInfo record);

    int insertSelective(ConferenceRecordInfo record);

    ConferenceRecordInfo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ConferenceRecordInfo record);

    int updateByPrimaryKey(ConferenceRecordInfo record);

    List<ConferenceRecordInfo> getPageListBySearch(List<String> ruids);

    long selectConfRecordsInfoCountByCondition(@Param("condition") ConferenceRecordSearch condition);

    List<ConferenceRecordInfo> selectByRuid(String ruid);

    void deleteByRuids(List<String> ruids);

    List<ConferenceRecordInfo> selectByIds(List<Long> ids);

    List<ConferenceRecordInfo> selectByProject(String project);
}
