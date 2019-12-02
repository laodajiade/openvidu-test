package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.DepartmentTree;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class GetChildDeviceListHandler extends RpcAbstractHandler {

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        JsonObject jsonObject = new  JsonObject();
        Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
        log.info("deptId:{}", userInfo.get("deptId"));
        Long orgId = Long.valueOf(String.valueOf(userInfo.get("deptId")));
        Long corpId = deviceDeptMapper.selectCorpByOrgId(orgId);
        Corporation corporation = corporationMapper.selectByPrimaryKey(corpId);
        List<DepartmentTree> deptLists = departmentMapper.selectByCorpId(corpId);
        List<DeviceDept>  devices = deviceDeptMapper.selectByCorpId(corpId);
        jsonObject.addProperty(ProtocolElements.GET_CHILD_DEVICE_LIST_CORP_NAME_PAPM,corporation.getCorpName());
        JsonArray deptList = new JsonArray();
        deptLists.forEach(dept->{
            JsonObject jsonDept = new JsonObject();
            jsonDept.addProperty(ProtocolElements.GET_CHILD_DEVICE_LIST_ORG_ID_PAPM, dept.getOrgId());
            jsonDept.addProperty(ProtocolElements.GET_CHILD_DEVICE_LIST_ORGANIZATION_NAME_PAPM, dept.getOrganizationName());
            deptList.add(jsonDept);
        });
        jsonObject.add(ProtocolElements.GET_CHILD_DEVICE_LIST_DEPT_LIST_PAPM, deptList);

        JsonArray deviceList = deviceList(devices);
        jsonObject.add(ProtocolElements.GET_CHILD_DEVICE_LIST_DEVICE_LIST_PAPM, deviceList);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);

    }
}
