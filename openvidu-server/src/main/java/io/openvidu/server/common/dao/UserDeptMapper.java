package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.RootDept;
import io.openvidu.server.common.pojo.UserDept;
import io.openvidu.server.common.pojo.UserDeptSearch;

public interface UserDeptMapper {

    String selectByUserId(Long userId);

    int deleteByPrimaryKey(Long id);

    int insert(UserDept record);

    int insertSelective(UserDept record);

    UserDept selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserDept record);

    int updateByPrimaryKey(UserDept record);

    UserDept selectBySearchCondition(UserDeptSearch search);

    RootDept selectRootDeptByUuid(String uuid);
}
