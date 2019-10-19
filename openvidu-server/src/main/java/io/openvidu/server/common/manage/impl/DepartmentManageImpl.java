package io.openvidu.server.common.manage.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;
import io.openvidu.server.utils.TreeToolUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author chosongi
 * @date 2019/10/18 22:35
 */
@Service
public class DepartmentManageImpl implements DepartmentManage {

    private final static Gson gson = new GsonBuilder().create();

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public JsonObject genDeptTreeJsonObj(@NotNull Long orgId) {
        Department rootDept = departmentMapper.selectByPrimaryKey(orgId);
        if (Objects.isNull(rootDept)) return null;

        List<DepartmentTree> deptList = departmentMapper.selectByCorpId(rootDept.getCorpId());
        DepartmentTree rootDeptTree = DepartmentTree.builder().orgId(rootDept.getId())
                .organizationName(rootDept.getDeptName()).build();

        return !CollectionUtils.isEmpty(deptList) ? gson.toJsonTree(new TreeToolUtils(Collections.singletonList(rootDeptTree),
                deptList.stream().filter(s -> s.getOrgId().compareTo(orgId) != 0).collect(Collectors.toList()))
                .getTree().get(0)).getAsJsonObject() : gson.toJsonTree(rootDeptTree).getAsJsonObject();
    }

}
