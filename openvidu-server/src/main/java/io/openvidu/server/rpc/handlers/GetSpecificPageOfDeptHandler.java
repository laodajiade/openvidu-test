package io.openvidu.server.rpc.handlers;

import com.github.pagehelper.PageInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.vo.DeptInfoVO;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.service.DepartmentService;
import io.openvidu.server.service.UserDeptService;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
public class GetSpecificPageOfDeptHandler extends RpcAbstractHandler {

    @Resource
    private DepartmentService departmentService;
    @Resource
    private UserDeptService userDeptService;


    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        boolean isChooseAll = getBooleanParam(request,"isChooseAll");
        Long deptId = getLongParam(request,"deptId");
        Integer pageNum = getIntOptionalParam(request,"pageNum");
        Integer pageSize = getIntOptionalParam(request,"pageSize");
        DeptInfoVO vo = new DeptInfoVO();
        vo.setIsChooseAll(isChooseAll);
        vo.setDeptId(deptId);
        vo.setProject(rpcConnection.getProject());

        if (isChooseAll) {
            vo.setPageNum(1);
            vo.setPageSize(Integer.MAX_VALUE);
        } else {
            if (Objects.isNull(pageNum) || Objects.isNull(pageSize)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
                return;
            }
            vo.setPageNum(pageNum);
            vo.setPageSize(pageSize);
        }

        List<Department> departments = departmentService.listByParentId(vo);
        JsonArray jsonArray = new JsonArray();
        if (!CollectionUtils.isEmpty(departments)) {
            // setHasSubOrg  setHasMember
            List<Long> deptids = departments.stream().map(Department::getId).collect(Collectors.toList());
            Set<Long> hasSubDeptId = departmentService.listByParentId(deptids).stream().map(Department::getParentId).collect(Collectors.toSet());
            Set<Long> hasMemberDeptId = userDeptService.distinctDeptId(deptids);
            for (Department department : departments) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("deptId",department.getId());
                jsonObject.addProperty("deptName",department.getDeptName());
                jsonObject.addProperty("parentId",department.getParentId());
                jsonObject.addProperty("hasSubOrg",hasSubDeptId.contains(department.getId()));
                jsonObject.addProperty("hasMember",hasMemberDeptId.contains(department.getId()));
                jsonArray.add(jsonObject);
            }
        }
        PageInfo<Department> pageInfo = new PageInfo<>(departments);
        JsonObject respJson = new JsonObject();
        respJson.addProperty("total",pageInfo.getTotal());
        respJson.addProperty("pageNum",pageInfo.getPageNum());
        respJson.addProperty("pageSize",pageInfo.getPageSize());
        respJson.addProperty("pages",pageInfo.getPages());
        respJson.add("list",jsonArray);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}
