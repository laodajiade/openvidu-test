package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Group;
import io.openvidu.server.common.pojo.UserGroup;
import io.openvidu.server.common.pojo.UserGroupVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserGroupMapper {

    int insert(UserGroup record);

    int deleteByPrimaryKey(Long id);

    List<UserGroupVo> selectListByGroupid(@Param("groupIds") List<Long> groupIds, @Param("notInUser") Set<Long> notInUser,
                                          @Param("visibleUsers") Set<Long> visibleUsers);

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

    List<Group> selectByCorpIds(Long corpId);

    /**
     * 查询群组中的用户是否存在
     * @return
     */
    List<Group> selectUserInGroup(Map<String,Object> map);

}
