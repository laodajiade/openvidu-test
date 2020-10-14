package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.ConferenceJobMapper;
import io.openvidu.server.common.manage.ConferenceJobManage;
import io.openvidu.server.common.pojo.ConferenceJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ConferenceJobManageImpl implements ConferenceJobManage {
    @Resource
    private ConferenceJobMapper conferenceJobMapper;

    @Override
    public int deleteByPrimaryKey(Long id) {
        return conferenceJobMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(ConferenceJob record) {
        return conferenceJobMapper.insert(record);
    }

    @Override
    public int insertSelective(ConferenceJob record) {
        return conferenceJobMapper.insertSelective(record);
    }

    @Override
    public ConferenceJob selectByPrimaryKey(Long id) {
        return conferenceJobMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByPrimaryKeySelective(ConferenceJob record) {
        return conferenceJobMapper.updateByPrimaryKeySelective(record);
    }

    @Override
    public int updateByPrimaryKey(ConferenceJob record) {
        return conferenceJobMapper.updateByPrimaryKey(record);
    }
    @Override
    public void batchInsert(List<ConferenceJob> list) {
        conferenceJobMapper.batchInsert(list);
    }

    @Override
    public List<Long> deleteConferenceJobByRuid(List<String> ruids) {
        List<Long> jobIds = conferenceJobMapper.getJobIdsByRuids(ruids);
        if (Objects.nonNull(jobIds) && !jobIds.isEmpty()) {
            conferenceJobMapper.batchDeleteByRuids(ruids);
        }
        return jobIds;
    }

    @Override
    public List<Long> deleteConferenceJobByRuid(String ruid) {
        return deleteConferenceJobByRuid(Collections.singletonList(ruid));
    }

    @Override
    public void deleteConferenceJobByJobId(Long jobId) {
        conferenceJobMapper.deleteConferenceJobByJobId(jobId);
    }

}
