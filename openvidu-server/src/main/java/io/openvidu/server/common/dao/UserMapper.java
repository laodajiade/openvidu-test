package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.AllUserInfo;
import io.openvidu.server.common.pojo.SoftUser;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.common.pojo.vo.UserDeptInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    UserDeviceDeptInfo queryUserInfoByUserId(Long userId);

    List<SoftUser> selectSoftUserByDeptId(Long deptId);

    List<SoftUser> selectSoftUserByDeptIds(List<Long> userIds);

    List<User> selectUserByUuidList(List<String> uuids);

    List<AllUserInfo> selectAllUserList(@Param("deptId") Long deptId, @Param("notInUser") Set<Long> notInUser, @Param("visibleUser") Set<Long> visibleUser);

    List<AllUserInfo> selectAllUserByUuidList(List<String> uuids);

    List<AllUserInfo> selectAllUserByUserIdsList(List<Long> userIds);

    User selectAdminUserByProject(String project);

    List<User> selectCallUserByUuidList(List<String> uuids);

    /**
     * 根据部门id查询 部门下的所有SIP用户
     * @param deptId
     * @return
     */
    List<AllUserInfo> selectSipUserList(Long deptId);

    /**
     * 查询用户信息以及所在的部门
     * @param uuid
     * @return
     */
    UserDeptInfo selectUserDept(String uuid);


    /**
     * 查询硬终端信息
     * @param uuid
     * @return
     */
    User selectTerminalInfo(String uuid);


    /**
     * 查询sip用户的设备号
     * @param uuid
     * @return
     */
    String selectSipUserNumber(String uuid);
}
