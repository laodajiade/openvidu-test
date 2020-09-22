package io.openvidu.server.common.manage.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.dao.DeviceDeptMapper;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.common.pojo.DeviceDeptSearch;
import io.openvidu.server.service.DepartmentService;
import io.openvidu.server.utils.TreeToolUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author geedow
 * @date 2019/10/18 22:35
 */
@Service
public class DepartmentManageImpl implements DepartmentManage {

    private final static Gson gson = new GsonBuilder().create();

    @Resource
    private DepartmentMapper departmentMapper;

    @Resource
    private DeviceDeptMapper deviceDeptMapper;

    @Autowired
    private DepartmentService departmentService;

    @Override
    public JsonObject genDeptTreeJsonObj(@NotNull Long orgId) {
        Department rootDept = departmentMapper.selectByPrimaryKey(orgId);
        if (Objects.isNull(rootDept)) return null;
        DepartmentTree rootDeptTree = DepartmentTree.builder().orgId(rootDept.getId())
                .organizationName(rootDept.getDeptName()).build();

        List<DepartmentTree> deptBeforeFilterList = departmentMapper.selectByCorpId(rootDept.getCorpId());
        List<DepartmentTree> deptAfterFilterList = !CollectionUtils.isEmpty(deptBeforeFilterList) ?
                deptBeforeFilterList.stream().filter(s -> !Objects.isNull(s.getOrgId()) && s.getOrgId().compareTo(orgId) != 0)
                        .collect(Collectors.toList()) : null;

        return !CollectionUtils.isEmpty(deptAfterFilterList) ? gson.toJsonTree(new TreeToolUtils(Collections.singletonList(rootDeptTree),
                deptAfterFilterList).getTree().get(0)).getAsJsonObject() : gson.toJsonTree(rootDeptTree).getAsJsonObject();
    }

    @Override
    public List<Department> getSubFirstLevelDepts(Long deptId) {
        List<Department> departments = new ArrayList<>();
        Department userCurrentDept = departmentMapper.selectByPrimaryKey(deptId);
        if (Objects.isNull(userCurrentDept))
            return departments;
        departments.add(userCurrentDept);

        List<Department> subFirstLevelDepts = departmentMapper.getSubFirstLevelDepts(deptId);
        if (!CollectionUtils.isEmpty(subFirstLevelDepts))
            departments.addAll(subFirstLevelDepts);

        return departments;
    }

    @Override
    public List<Long> getSubDeptIds(@NotNull Long deptId) {
        Department rootDept = departmentMapper.selectByPrimaryKey(deptId);
        if (rootDept == null) return null;

        List<Long> subDeptIds = new ArrayList<>();
        List<DepartmentTree> deptList = departmentMapper.selectByCorpId(rootDept.getCorpId());
        if (!CollectionUtils.isEmpty(deptList)) {
            TreeToolUtils treeToolUtils = new TreeToolUtils(
                    Collections.singletonList(DepartmentTree.builder().orgId(deptId).organizationName(rootDept.getDeptName()).build()),
                    deptList.stream().filter(s -> !Objects.isNull(s.getOrgId()) && s.getOrgId().compareTo(deptId) != 0).collect(Collectors.toList()));
            treeToolUtils.getTree();
            subDeptIds = treeToolUtils.getSubDeptIds();
        }

        subDeptIds.add(deptId);

        return subDeptIds;
    }

    @Override
    public DeviceDept getDeviceDeptBySerialNum(String devSerialNum) {
        DeviceDeptSearch deviceDeptSearch = new DeviceDeptSearch();
        deviceDeptSearch.setSerialNumber(devSerialNum);
        List<DeviceDept> deviceDepts = deviceDeptMapper.selectBySearchCondition(deviceDeptSearch);
        return !CollectionUtils.isEmpty(deviceDepts) ? deviceDepts.get(0) : null;
    }

    /**
     * 递归获取 deptIds 下的所有子部门,并返回包含本部门已经子部门的id列表
     */
    @Override
    public List<Long> getAllChildDept(List<Long> deptIds) {

        Set<Long> depts = new HashSet<>();

        while (depts.addAll(deptIds)) {
            List<Department> departments = departmentService.listByParentId(deptIds);
            deptIds = departments.stream().map(Department::getId).collect(Collectors.toList());
        }

        return new ArrayList<>(depts);
    }

}
