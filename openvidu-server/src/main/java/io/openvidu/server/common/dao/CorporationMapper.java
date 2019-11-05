package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Corporation;

public interface CorporationMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Corporation record);

    int insertSelective(Corporation record);

    Corporation selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Corporation record);

    int updateByPrimaryKey(Corporation record);
}