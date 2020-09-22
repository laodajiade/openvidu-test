package io.openvidu.server.utils;

import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.SimpleDeptInfo;

import java.util.List;
import java.util.stream.Collectors;

public class SimpleDeptInfoHelper {


    public static SimpleDeptInfo coverFromDept(Department dept) {
        SimpleDeptInfo info = new SimpleDeptInfo();

        info.setDeptId(dept.getId());
        info.setDeptName(dept.getDeptName());
        info.setParentId(dept.getParentId());
        info.setHasSubOrg(dept.getHasSubOrg());
        info.setHasMember(dept.getHasSubOrg());
        info.setNumOfEmp(dept.getNumOfEmp());

        return info;
    }

    public static List<SimpleDeptInfo> coverFromDept(List<Department> users) {
        return users.stream().map(SimpleDeptInfoHelper::coverFromDept).collect(Collectors.toList());
    }
}
