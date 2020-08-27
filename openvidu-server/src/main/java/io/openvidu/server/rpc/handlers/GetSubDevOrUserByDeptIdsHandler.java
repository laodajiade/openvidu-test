package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.common.pojo.SoftUser;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author chosongi
 * @date 2020/4/22 22:34
 */
@Slf4j
@Service
public class GetSubDevOrUserByDeptIdsHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        List<Long> targetIds = getLongListParam(request, ProtocolElements.GETSUBDEVORUSERBYDEPTIDS_ORGIDS_PARAM);

        if (CollectionUtils.isEmpty(targetIds)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        List<DeviceDept> devices = deviceDeptMapper.selectByDeptIds(targetIds);
        List<SoftUser> softUsers = userMapper.selectSoftUserByDeptIds(targetIds);
        JsonObject jsonObject = new  JsonObject();
        JsonArray accountList = accountList(devices,softUsers);
        jsonObject.add(ProtocolElements.GET_SUB_DEVORUSER_ACCOUNT_LIST_PARAM, accountList);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }
}
