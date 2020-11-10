package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.NotEndPartHistory;
import io.openvidu.server.common.pojo.StatisticsDurationInfo;
import io.openvidu.server.common.pojo.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConferencePartHistoryMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ConferencePartHistory record);

    int insertSelective(ConferencePartHistory record);

    ConferencePartHistory selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ConferencePartHistory record);

    int updateByPrimaryKey(ConferencePartHistory record);

    List<ConferencePartHistory> selectByCondition(ConferencePartHistory search);

    void updatePartHistroy(ConferencePartHistory update);

    void updateNotEndPartHistroy(List<ConferencePartHistory> list);

    List<ConferencePartHistory> selectConfPartHistoryByRuids(List<String> ruids);

    List<User> selectUserByRuid(String ruid);

    @Delete("delete from sd_conf_part_history where ruid = #{ruid}")
    int deleteByRuid(String ruid);

    List<StatisticsDurationInfo> selectStatisticsDuration(@Param("project") String project, @Param("uuid") String uuid);

    List<NotEndPartHistory> selectNotEndPartHistory();

    List<ConferencePartHistory> selectProcessPartHistory(@Param("project") String project);
}
