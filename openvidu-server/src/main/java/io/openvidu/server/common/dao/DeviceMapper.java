package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Device;

public interface DeviceMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Device record);

    int insertSelective(Device record);

    Device selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Device record);

    int updateByPrimaryKey(Device record);
}