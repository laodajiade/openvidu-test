package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * @author geedow
 * @date 2019/11/5 20:18
 */
@Slf4j
@Service
public class GetOrgListHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
        log.info("deptId:{}", userInfo.get("deptId"));
        Long userDeptId = Long.valueOf(String.valueOf(userInfo.get("deptId")));

        // TODO eliminate unnecessary corp info according to the protocol
        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.GET_ORG_ID_PARAM, 0);
        params.addProperty(ProtocolElements.GET_ORG_NAME_PARAM, "速递科技");

        JsonArray orgList = new JsonArray();
        List<Department> departments = departmentManage.getSubFirstLevelDepts(userDeptId);
        if (!CollectionUtils.isEmpty(departments)) {
            departments.forEach(dept -> {
                JsonObject org = new JsonObject();
                org.addProperty(ProtocolElements.GET_ORG_ID_PARAM, dept.getId());
                org.addProperty(ProtocolElements.GET_ORG_NAME_PARAM, dept.getDeptName());
                orgList.add(org);
            });
        }
        params.add(ProtocolElements.GET_ORG_LIST_PARAM, orgList);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), params);
    }
}
