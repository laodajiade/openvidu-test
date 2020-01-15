package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.UserDevice;

public interface UserDeviceMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserDevice record);

    int insertSelective(UserDevice record);

    UserDevice selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserDevice record);

    int updateByPrimaryKey(UserDevice record);

    UserDevice selectByCondition(UserDevice search);
}