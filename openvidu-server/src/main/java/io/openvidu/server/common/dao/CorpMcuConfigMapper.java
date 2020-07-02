package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.CorpMcuConfig;

public interface CorpMcuConfigMapper {

    int deleteByPrimaryKey(Long id);

    int insert(CorpMcuConfig record);

    int insertSelective(CorpMcuConfig record);

    CorpMcuConfig selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(CorpMcuConfig record);

    int updateByPrimaryKey(CorpMcuConfig record);

    CorpMcuConfig selectByProject(String project);

    void updateByProject(CorpMcuConfig corpMcuConfig);
}