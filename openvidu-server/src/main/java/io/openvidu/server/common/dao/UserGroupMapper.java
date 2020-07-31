package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.UserGroup;

import java.util.List;

public interface UserGroupMapper {

    int insert(UserGroup record);

    int deleteByPrimaryKey(Long id);

    List<UserGroup> selectListByGroupid(Long orgId);

    List<UserGroup> selectListByUserId(Long userId);

    int deleteByGroupId(Long groupId);

    int deleteByGroupAndUserId(Long groupId, Long userId);

    int deleteByUserId(Long userId);



    List<UserGroup> selectListByGroupids(Long orgId);

    int updateByUserId(UserGroup userDept);

    Integer selectByProject(String project);

    Long selectByUserId(Long userId);

    int insertSelective(UserGroup record);

    UserGroup selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserGroup record);

    int updateByPrimaryKey(UserGroup record);
}