package io.openvidu.server.common.manage;

import com.google.gson.JsonObject;
import io.openvidu.server.common.pojo.Department;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author geedow
 * @date 2019/10/18 22:34
 */
public interface DepartmentManage {
    JsonObject genDeptTreeJsonObj(@NotNull Long orgId);

    List<Department> getSubFirstLevelDepts(Long deptId);

    List<Long> getSubDeptIds(Long deptId);
}
