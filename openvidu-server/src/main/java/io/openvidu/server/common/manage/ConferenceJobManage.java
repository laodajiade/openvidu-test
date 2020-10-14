package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.ConferenceJob;

import java.util.List;

public interface ConferenceJobManage {
    int deleteByPrimaryKey(Long id);

    int insert(ConferenceJob record);

    int insertSelective(ConferenceJob record);

    ConferenceJob selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(ConferenceJob record);

    int updateByPrimaryKey(ConferenceJob record);

    void batchInsert(List<ConferenceJob> list);

    List<Long> deleteConferenceJobByRuid(List<String> ruids);

    List<Long> deleteConferenceJobByRuid(String ruid);

    void deleteConferenceJobByJobId(Long jobId);
}
