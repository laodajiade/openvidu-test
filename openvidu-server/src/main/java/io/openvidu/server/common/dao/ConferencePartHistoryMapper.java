package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.common.pojo.User;
import org.apache.ibatis.annotations.Delete;

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

    List<ConferencePartHistory> selectConfPartHistoryByRuids(List<String> ruids);

    List<User> selectUserByRuid(String ruid);

    @Delete("delete from sd_conf_part_history where ruid = #{ruid}")
    int deleteByRuid(String ruid);
}
