package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.UserMapper;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.User;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserManageImpl implements UserManage {
    @Resource
    private UserMapper userMapper;

    @Resource
    private DepartmentManage departmentManage;

    @Override
    public List<User> getSubUserByDeptId(Long deptId) {
        List<Long> subDeptIds = departmentManage.getSubDeptIds(deptId);
        if (CollectionUtils.isEmpty(subDeptIds)) return null;

        List<Long> userIds = userMapper.selectUserIdsByDeptIds(subDeptIds);
        return !CollectionUtils.isEmpty(userIds) ? userMapper.getUsersByUserIdsList(userIds) : null;
    }
}
