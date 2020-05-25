package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.RoleMapper;
import io.openvidu.server.common.dao.UserMapper;
import io.openvidu.server.common.dao.UserRoleMapper;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.Role;
import io.openvidu.server.common.pojo.User;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

@Service
public class UserManageImpl implements UserManage {
    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private DepartmentManage departmentManage;

    @Resource
    private UserRoleMapper userRoleMapper;


    @Override
    public List<User> getSubUserByDeptId(Long deptId) {
        List<Long> subDeptIds = departmentManage.getSubDeptIds(deptId);
        if (CollectionUtils.isEmpty(subDeptIds)) return null;

        List<Long> userIds = userMapper.selectUserIdsByDeptIds(subDeptIds);
        return !CollectionUtils.isEmpty(userIds) ? userMapper.getUsersByUserIdsList(userIds) : null;
    }

    @Override
    public User queryByUuid(String uuid) {
        return userMapper.selectByUUID(uuid);
    }

    @Override
    public boolean isAdmin(String uuid) {
        User user = this.queryByUuid(uuid);
        if (Objects.isNull(user)) return false;
        if (Objects.equals("administrator", uuid)) return true;

        Long roleId = userRoleMapper.selectByUserId(user.getId());
        Role role = roleMapper.selectByPrimaryKey(roleId);

        return (Objects.nonNull(role) && Objects.equals(role.getRoleName(), "admin"));
    }

}
