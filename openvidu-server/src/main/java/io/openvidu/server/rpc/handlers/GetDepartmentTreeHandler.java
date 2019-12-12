package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.TreeToolUtils;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GetDepartmentTreeHandler extends RpcAbstractHandler {

    @Resource
    private DepartmentManage departmentManage;

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Department rootDept;
        String devSerialNum = rpcConnection.getSerialNumber();
        if (!StringUtils.isEmpty(devSerialNum)) {
            DeviceDept deviceDept = departmentManage.getDeviceDeptBySerialNum(devSerialNum);
            if (Objects.isNull(deviceDept)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(),
                        request.getId(), null, ErrorCodeEnum.DEVICE_NOT_FOUND);
                return;
            } else {
                rootDept = departmentMapper.selectByPrimaryKey(deviceDept.getDeptId());
            }
        } else {
            // request not from vhd device
            Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
            Long orgId = Long.valueOf(String.valueOf(userInfo.get("deptId")));
            rootDept = departmentMapper.selectByPrimaryKey(orgId);
        }

        List<DepartmentTree> deptList = departmentMapper.selectByCorpId(rootDept.getCorpId());
        DepartmentTree rootDeptTree = DepartmentTree.builder().orgId(rootDept.getId()).parentId(rootDept.getParentId())
                .organizationName(rootDept.getDeptName()).build();

        JsonObject object = !CollectionUtils.isEmpty(deptList) ? gson.toJsonTree(new TreeToolUtils(Collections.singletonList(rootDeptTree),
                deptList.stream().filter(s -> !Objects.isNull(s.getParentId()) && s.getOrgId().compareTo(rootDept.getId()) != 0).collect(Collectors.toList()))
                .getTree().get(0)).getAsJsonObject()
                : gson.toJsonTree(rootDeptTree).getAsJsonObject();

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), object);

    }
}
