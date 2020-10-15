package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.UserLoginHistory;

public interface UserLoginHistoryMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserLoginHistory record);

    int insertSelective(UserLoginHistory record);

    UserLoginHistory selectByPrimaryKey(Long id);

    UserLoginHistory selectByCondition(UserLoginHistory condition);

    int updateByPrimaryKeySelective(UserLoginHistory record);

    int updateByPrimaryKey(UserLoginHistory record);
}