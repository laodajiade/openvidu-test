package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.vo.CallHistoryVo;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


/**
 * @author even
 * @date 2021/1/18 16:52
 */
@Slf4j
@Service
public class GetCallListHandler extends RpcAbstractHandler {
    @Autowired
    SessionManager sessionManager;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String ruid = getStringParam(request, ProtocolElements.GET_CALL_LIST_RUID_PARAM);
        List<CallHistoryVo> callHistories = callHistoryMapper.getCallHistoryList(ruid);
        Optional<Session> optSession = sessionManager.getSessions().stream().filter(x -> x.getRuid().equals(ruid)).findFirst();
        if (optSession.isPresent()) {
            Set<Participant> participants = optSession.get().getParticipants();
            callHistories = callHistories.stream().filter(new Predicate<CallHistoryVo>() {
                @Override
                public boolean test(CallHistoryVo callHistoryVo) {
                    for (Participant participant : participants) {
                        if (callHistoryVo.getUuid().equals( participant.getUuid())) {
                            return false;
                        }
                    }
                    return true;
                }
            }).collect(Collectors.toList());
        }
        JSONObject respJson = new JSONObject();
        respJson.put("list", callHistories);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}
