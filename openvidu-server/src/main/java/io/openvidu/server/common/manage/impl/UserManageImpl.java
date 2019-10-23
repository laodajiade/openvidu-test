package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.dao.UserMapper;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.utils.TreeToolUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserManageImpl implements UserManage {
    @Resource
    private UserMapper userMapper;

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public List<User> getSubUserByDeptId(Long deptId) {
        Department rootDept = departmentMapper.selectByPrimaryKey(deptId);
        if (rootDept == null) return null;

        List<Long> subDeptIds = new ArrayList<>();
        List<DepartmentTree> deptList = departmentMapper.selectByCorpId(rootDept.getCorpId());
        if (!CollectionUtils.isEmpty(deptList)) {
            TreeToolUtils treeToolUtils = new TreeToolUtils(
                    Collections.singletonList(DepartmentTree.builder().orgId(deptId).organizationName(rootDept.getDeptName()).build()),
                    deptList.stream().filter(s -> s.getOrgId().compareTo(deptId) != 0).collect(Collectors.toList()));
            treeToolUtils.getTree();
            subDeptIds = treeToolUtils.getSubDeptIds();
        }
        subDeptIds.add(deptId);

        List<Long> userIds = userMapper.selectUserIdsByDeptIds(subDeptIds);
        return !CollectionUtils.isEmpty(userIds) ?
                userMapper.getUsersByUserIdsList(userIds) : null;
    }
}
