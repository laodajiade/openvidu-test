package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.AllUserInfo;
import io.openvidu.server.common.pojo.SoftUser;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;

import java.util.List;

public interface UserMapper {

    int deleteByPrimaryKey(Long id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    List<User> selectByPrimaryKeys(List<Long> ids);

    User selectByUUID(String uuid);

    List<Long> selectUserIdsByDeptIds(List<Long> subDeptIds);

    List<User> getUsersByUserIdsList(List<Long> userIds);

    List<UserDeviceDeptInfo> queryUserInfoByUserIds(List<Long> userIds);

    List<SoftUser> selectSoftUserByDeptId(Long deptId);

    List<SoftUser> selectSoftUserByDeptIds(List<Long> userIds);

    List<User> selectUserByUuidList(List<String> uuids);

    List<AllUserInfo> selectAllUserList(Long deptId);

    List<AllUserInfo> selectAllUserByUuidList(List<String> uuids);

    List<AllUserInfo> selectAllUserByUserIdsList(List<Long> userIds);
}
