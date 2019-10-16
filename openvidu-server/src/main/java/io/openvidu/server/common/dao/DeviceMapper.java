package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;

public interface DeviceMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Device record);

    int insertSelective(Device record);

    Device selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Device record);

    int updateByPrimaryKey(Device record);

    Device selectBySearchCondition(DeviceSearch search);
}