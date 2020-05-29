package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ParticipantStatusEnum;
import io.openvidu.server.common.enums.UserType;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class GetMeetingRecordDetailHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String ruid = getStringParam(request, ProtocolElements.GETMEETINGSRECORDDETAIL_RUID_PARAM);
        JsonObject respObj = new JsonObject();
        JsonArray parts = new JsonArray();

        ConferencePartHistory search = new ConferencePartHistory();
        search.setRuid(ruid);
        search.setStatus(ParticipantStatusEnum.PROCESS.getStatus());
        List<ConferencePartHistory> confPartHistoryList = roomManage.getConfHistoryRecordsByCondition(search);
        if (!CollectionUtils.isEmpty(confPartHistoryList)) {
            confPartHistoryList.forEach(conferencePartHistory -> {
                JsonObject partRecObj = new JsonObject();
                partRecObj.addProperty("account", conferencePartHistory.getUuid());
                partRecObj.addProperty("username", conferencePartHistory.getUsername());
                partRecObj.addProperty("userType", conferencePartHistory.getUserType().compareTo(0) == 0 ?
                        UserType.register.name() : UserType.tourist.name());
                partRecObj.addProperty("startTime", conferencePartHistory.getStartTime().getTime());
                partRecObj.addProperty("terminalType", conferencePartHistory.getTerminalType());
                parts.add(partRecObj);
            });
        }

        respObj.add("participants", parts);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }
}
