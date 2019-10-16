package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.UserDept;

public interface UserDeptMapper {

    int deleteByPrimaryKey(Long id);

    int insert(UserDept record);

    int insertSelective(UserDept record);

    UserDept selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserDept record);

    int updateByPrimaryKey(UserDept record);
}