package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.DeviceDept;

public interface DeviceDeptMapper {

    int deleteByPrimaryKey(Long id);

    int insert(DeviceDept record);

    int insertSelective(DeviceDept record);

    DeviceDept selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DeviceDept record);

    int updateByPrimaryKey(DeviceDept record);
}