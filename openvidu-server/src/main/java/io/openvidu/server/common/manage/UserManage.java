package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.Role;
import io.openvidu.server.common.pojo.User;
import java.util.List;

public interface UserManage {
    List<User> getSubUserByDeptId(Long deptId);

    User queryByUuid(String uuid);

    boolean isAdmin(String uuid);

    Role getUserRoleById(Long roleId);
}
