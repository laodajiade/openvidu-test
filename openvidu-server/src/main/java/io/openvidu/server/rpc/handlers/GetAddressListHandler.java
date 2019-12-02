package io.openvidu.server.rpc.handlers;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.DepartmentMapper;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.common.pojo.DeviceDeptSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;


import javax.annotation.Resource;
import java.util.List;


@Slf4j
@Service
public class GetAddressListHandler extends RpcAbstractHandler {

    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Long orgId = getLongParam(request, ProtocolElements.GETADDRESSLIST_ORG_ID);
        DeviceDeptSearch search = new DeviceDeptSearch();
        search.setDeptId(orgId);
        List<DeviceDept> devices = deviceDeptMapper.selectBySearchCondition(search);
        JsonObject jsonObject = new  JsonObject();
        JsonArray deviceList=deviceList(devices);
        jsonObject.add(ProtocolElements.GET_CHILD_DEVICE_LIST_DEVICE_LIST_PAPM, deviceList);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }
}
