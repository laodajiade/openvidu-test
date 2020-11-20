package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.RoleMapper;
import io.openvidu.server.common.dao.UserDeptMapper;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.manage.RoleManage;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

@Service
public class RoleManageImpl implements RoleManage {


    @Resource
    private RoleMapper roleMapper;

    @Resource
    private DepartmentManage departmentManage;

    @Resource
    private UserDeptMapper userDeptMapper;

    /**
     * @return 返回后台的权限列表，如果全部可见则List.isEmpty
     */
    @Override
    public List<Long> getDeptLimit(Long userId) {
        Integer deptLimitRole = roleMapper.getDeptLimitByUserId(userId);
        if (deptLimitRole != null && deptLimitRole == 1) {
            return departmentManage.getAllChildDept(singletonList(userDeptMapper.selectInUserId(singletonList(userId)).get(0).getDeptId()));
        }
        return Collections.emptyList();
    }
}
