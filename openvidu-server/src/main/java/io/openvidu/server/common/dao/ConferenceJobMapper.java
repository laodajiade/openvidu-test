package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.ConferenceJob;

import java.util.List;

public interface ConferenceJobMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ConferenceJob record);

    int insertSelective(ConferenceJob record);

    ConferenceJob selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ConferenceJob record);

    int updateByPrimaryKey(ConferenceJob record);

    void batchInsert(List<ConferenceJob> list);

    List<Long> getJobIdsByRuids(List<String> ruids);

    void batchDeleteByRuids(List<String> ruids);

    void deleteConferenceJobByJobId(Long jobId);
}