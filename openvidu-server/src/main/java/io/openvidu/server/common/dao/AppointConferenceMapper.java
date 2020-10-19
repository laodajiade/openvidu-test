package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.AppointConferenceExample;
import java.util.List;

import io.openvidu.server.common.pojo.Conference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface AppointConferenceMapper {
    long countByExample(AppointConferenceExample example);

    int deleteByExample(AppointConferenceExample example);

    int deleteByPrimaryKey(Long id);

    int insert(AppointConference record);

    int insertSelective(AppointConference record);

    List<AppointConference> selectByExample(AppointConferenceExample example);

    AppointConference selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AppointConference record, @Param("example") AppointConferenceExample example);

    int updateByExample(@Param("record") AppointConference record, @Param("example") AppointConferenceExample example);

    int updateByPrimaryKeySelective(AppointConference record);

    int updateByPrimaryKey(AppointConference record);

    List<AppointConference> getConflictAppointConferenceList(AppointConference condition);

    List<AppointConference> pendingAboutAppointment(Long userId);

    void changeStatusByRuid(@Param("status") int status, @Param("ruid") String ruid);
}