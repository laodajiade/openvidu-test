package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.RootDept;
import io.openvidu.server.common.pojo.UserDept;
import io.openvidu.server.common.pojo.UserDeptSearch;

import java.util.List;

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

    List<UserDept> selectByDeptIdsList(List<Long> deptIds);

    List<UserDept> selectInUserId(List<Long> userIds);

    List<Long> selectUserByDeptIdsList(List<Long> deptIds);

    List<Long> selectDeviceByDeptIdsList(List<Long> deptIds);

}
