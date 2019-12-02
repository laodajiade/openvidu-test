package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.DepartmentTree;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.TreeToolUtils;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


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
    private DepartmentMapper departmentMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {

        JsonObject jsonObject = new  JsonObject();
        Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
        log.info("deptId:{}", userInfo.get("deptId"));
        Long orgId = Long.valueOf(String.valueOf(userInfo.get("deptId")));
        Department rootDept = departmentMapper.selectByPrimaryKey(orgId);
        Long corpId = deviceDeptMapper.selectCorpByOrgId(orgId);
        Corporation corporation = corporationMapper.selectByPrimaryKey(corpId);
        jsonObject.addProperty(ProtocolElements.GET_DEPARTMENT_TREE_CORP_NAME_PAPM, corporation.getCorpName());
        List<DepartmentTree> deptList = departmentMapper.selectByCorpId(corpId);


        DepartmentTree rootDeptTree = DepartmentTree.builder().orgId(rootDept.getId()).parentId(rootDept.getParentId())

                .organizationName(rootDept.getDeptName()).build();

        JsonObject object = !CollectionUtils.isEmpty(deptList) ? gson.toJsonTree(new TreeToolUtils(Collections.singletonList(rootDeptTree),
                deptList.stream().filter(s -> !Objects.isNull(s.getParentId()) && s.getOrgId().compareTo(orgId) != 0).collect(Collectors.toList()))
                .getTree().get(0)).getAsJsonObject()
                : gson.toJsonTree(rootDeptTree).getAsJsonObject();

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(object);
        jsonObject.add(ProtocolElements.GET_DEPARTMENT_TREE_ORGANIZATION_LIST_PAPM, jsonArray);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);

    }
}
