package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.AppointParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Mapper
@Repository
public interface AppointParticipantMapper {
    int deleteByPrimaryKey(Long id);

    int insert(AppointParticipant record);

    int insertSelective(AppointParticipant record);

    AppointParticipant selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(AppointParticipant record);

    int updateByPrimaryKey(AppointParticipant record);

    void batchInsert(List<AppointParticipant> appointParticipantList);

    void deleteByConferenceRuid(String ruid);

    List<AppointParticipant> selectByCondition(AppointParticipant appointSearch);

    List<AppointParticipant> selectByRuids(List<String> confRuids);

    default List<AppointParticipant> selectListByRuid(String ruid) {
        return selectByRuids(Collections.singletonList(ruid));
    }

    void endAppointStatusByRuids(List<String> expiredAppointRuids);
}