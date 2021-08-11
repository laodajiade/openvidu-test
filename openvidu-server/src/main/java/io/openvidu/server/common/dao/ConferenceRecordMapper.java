package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConferenceRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    void deleteByRoomId(String roomId);

    void updatePreRecordErrorStatus(ConferenceRecord record);

    /**
     * 根据ruId 查询录制状态
     * @param ruId
     * @return
     */
    ConferenceRecord getByRuIdRecordStatus(String ruId);

    @Select("select count(*) from sd_conference_record a ,sd_conference b where a.ruid= b.ruid and a.status != 2 and b.status != 2 and b.project=#{project}")
    int countRecordNumByProject(@Param("project") String project);
}
