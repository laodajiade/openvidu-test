package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.User;
import java.util.List;

public interface UserManage {
    List<User> getSubUserByDeptId(Long deptId);
}
