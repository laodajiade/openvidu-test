package io.openvidu.server.rpc.handlers;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.SimpleDeptInfo;
import io.openvidu.server.common.pojo.vo.DeptInfoVO;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.service.DepartmentService;
import io.openvidu.server.service.UserDeptService;
import io.openvidu.server.utils.SimpleDeptInfoHelper;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
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
            vo.setPageSize(-1);
        } else {
            if (Objects.isNull(pageNum) || Objects.isNull(pageSize)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
                return;
            }
            vo.setPageNum(pageNum);
            vo.setPageSize(pageSize);
        }
        IPage<Department> page = departmentService.listByParentId(vo);
        List<Department> records = page.getRecords();
        List<SimpleDeptInfo> result = new ArrayList<>();
        JsonArray jsonArray = new JsonArray();
        if (!CollectionUtils.isEmpty(records)) {
            result = SimpleDeptInfoHelper.coverFromDept(records);
            // setHasSubOrg  setHasMember
            List<Long> deptids = result.stream().map(SimpleDeptInfo::getDeptId).collect(Collectors.toList());
            Set<Long> hasSubDeptId = departmentService.listByParentId(deptids).stream().map(Department::getParentId).collect(Collectors.toSet());
            Set<Long> hasMemberDeptId = userDeptService.distinctDeptId(deptids);
            for (SimpleDeptInfo simpleDeptInfo : result) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("deptId",simpleDeptInfo.getDeptId());
                jsonObject.addProperty("deptName",simpleDeptInfo.getDeptName());
                jsonObject.addProperty("parentId",simpleDeptInfo.getParentId());
                jsonObject.addProperty("hasSubOrg",hasSubDeptId.contains(simpleDeptInfo.getDeptId()));
                jsonObject.addProperty("hasMember",hasMemberDeptId.contains(simpleDeptInfo.getDeptId()));
                jsonArray.add(jsonObject);
            }
        }
        JsonObject respJson = new JsonObject();
        respJson.addProperty("total",page.getSize());
        respJson.addProperty("pageNum",page.getCurrent());
        respJson.addProperty("pageSize",page.getSize());
        respJson.addProperty("pages",page.getPages());
        respJson.add("list",jsonArray);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}
