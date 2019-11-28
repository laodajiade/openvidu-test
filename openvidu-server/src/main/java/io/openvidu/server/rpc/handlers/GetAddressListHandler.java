package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.common.pojo.DeviceDeptSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.TreeToolUtils;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GetAddressListHandler extends RpcAbstractHandler {

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
        log.info("deptId:{}", userInfo.get("deptId"));
        Long orgId = Long.valueOf(String.valueOf(userInfo.get("deptId")));
        Department rootDept = departmentMapper.selectByPrimaryKey(orgId);


        if (Objects.isNull(rootDept)) return;

        List<DepartmentTree> deptList = departmentMapper.selectByCorpId(rootDept.getCorpId());
        deptList.forEach(this::setDevices);

        DepartmentTree rootDeptTree = DepartmentTree.builder().orgId(rootDept.getId()).parentId(rootDept.getParentId())

                .organizationName(rootDept.getDeptName()).build();

        setDevices(rootDeptTree);

        JsonObject jsonObject = !CollectionUtils.isEmpty(deptList) ? gson.toJsonTree(new TreeToolUtils(Collections.singletonList(rootDeptTree),
                deptList.stream().filter(s -> !Objects.isNull(s.getParentId()) && s.getOrgId().compareTo(orgId) != 0).collect(Collectors.toList()))
                .getTree().get(0)).getAsJsonObject()
                : gson.toJsonTree(rootDeptTree).getAsJsonObject();

        JsonArray DeptTree = new JsonArray();
        DeptTree.add(jsonObject);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), DeptTree);
    }

        public  void  setDevices(DepartmentTree dept){
            JsonArray jsondev = new JsonArray();
            DeviceDeptSearch search1 = new DeviceDeptSearch();
            search1.setDeptId(dept.getOrgId());
            List<DeviceDept> deviceDepts = deviceDeptMapper.selectBySearchCondition(search1);
            deviceDepts.forEach(device -> {
                JsonObject dev = new JsonObject();
                dev.addProperty(ProtocolElements.GETADDRESSLIST_SERIAL_NUMBER_PARAM, device.getSerialNumber());
                dev.addProperty(ProtocolElements.GETADDRESSLIST_DEVICE_NAME_PARAM, device.getDeviceName());
                for (RpcConnection rpcConnection : notificationService.getRpcConnections()) {

                    if (device.getSerialNumber().equals(rpcConnection.getSerialNumber())) {
                        dev.addProperty(ProtocolElements.GETADDRESSLIST_ACCOUNT_PARAM, rpcConnection.getUserUuid());
                        break;
                    }
                }
                jsondev.add(dev);

            });
            dept.setDeviceList(jsondev);
        }

}
