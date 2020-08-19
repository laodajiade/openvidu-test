package io.openvidu.server.rpc.handlers;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.common.pojo.DeviceDeptSearch;
import io.openvidu.server.common.pojo.SoftUser;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class GetSubDevOrUserHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Long orgId = getLongParam(request, ProtocolElements.GET_SUB_DEVORUSER_ORG_ID);
        DeviceDeptSearch search = new DeviceDeptSearch();
        search.setDeptId(orgId);
        List<DeviceDept> devices = deviceDeptMapper.selectBySearchCondition(search);
        List<SoftUser> softUsers = userMapper.selectSoftUserByDeptId(orgId);
        JsonObject jsonObject = new  JsonObject();
        JsonArray accountList = accountList(devices,softUsers);
        jsonObject.add(ProtocolElements.GET_SUB_DEVORUSER_ACCOUNT_LIST_PARAM, accountList);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }
}
