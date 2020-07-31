package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.CorpMcuConfig;

public interface CorpMcuConfigMapper {
    CorpMcuConfig selectByProject(String project);
}