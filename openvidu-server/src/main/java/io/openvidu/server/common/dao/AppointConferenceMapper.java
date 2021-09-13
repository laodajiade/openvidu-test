package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.AppointConferenceExample;
import io.openvidu.server.domain.AppointConferenceDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * AppointConferenceMapper继承基类
 */
@Mapper
@Repository
public interface AppointConferenceMapper extends MyBatisBaseDao<AppointConference, Long, AppointConferenceExample> {


    List<AppointConference> getConflictAppointConferenceList(AppointConference condition);

    List<AppointConference> pendingAboutAppointment(AppointConferenceDTO appointConference);

    List<AppointConference> pendingAboutAppointmentAdmin(AppointConferenceDTO appointConference);

    void changeStatusByRuid(@Param("status") int status, @Param("ruid") String ruid, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<AppointConference> getMaybeEndAppointment();
}