package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.Role;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.UserLoginHistory;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.core.Participant;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserManage {
    List<User> getSubUserByDeptId(Long deptId);

    User queryByUuid(String uuid);

    boolean isAdmin(String uuid);

    Role getUserRoleById(Long roleId);

    User getUserByUserId(Long userId);

    int modifyPassword(User user);

    void updateUserInfo(User update);

    Map<String, UserDeviceDeptInfo> getUserInfoInRoom(Set<Participant> participants);

    List<User> queryByUuidList(List<String> uuids);

    void saveUserLoginHistroy(UserLoginHistory userLoginHistory);

    User getAdminUserByProject(String project);

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
