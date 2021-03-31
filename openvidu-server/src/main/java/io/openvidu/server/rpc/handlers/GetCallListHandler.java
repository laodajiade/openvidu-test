package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.vo.CallHistoryVo;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * @author even
 * @date 2021/1/18 16:52
 */
@Slf4j
@Service
public class GetCallListHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String ruid = getStringParam(request, ProtocolElements.GET_CALL_LIST_RUID_PARAM);
        List<CallHistoryVo> callHistories = callHistoryMapper.getCallHistoryList(ruid);
        JSONObject respJson = new JSONObject();
        respJson.put("list", callHistories);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}
