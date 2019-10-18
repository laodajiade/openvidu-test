package io.openvidu.server.common.manage;

import com.google.gson.JsonObject;

import javax.validation.constraints.NotNull;

/**
 * @author chosongi
 * @date 2019/10/18 22:34
 */
public interface DepartmentManage {
    JsonObject genDeptTreeJsonObj(@NotNull Long orgId);
}
