package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantStatusEnum;
import io.openvidu.server.common.enums.UserType;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.apache.commons.lang.StringUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Service
public class GetMeetingRecordDetailHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String ruid = getStringParam(request, ProtocolElements.GETMEETINGSRECORDDETAIL_RUID_PARAM);
        JsonObject respObj = new JsonObject();
        JsonArray parts = new JsonArray();

        Conference conference = roomManage.getConferenceByRuid(ruid);
        if (Objects.isNull(conference)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }
        respObj.addProperty("roomId", conference.getRoomId());
        respObj.addProperty("createAt", conference.getStartTime().getTime());

        String mixParam = getStringOptionalParam(request, ProtocolElements.GETMEETINGSRECORDDETAIL_MIX_PARAM);
        ConferencePartHistory search = new ConferencePartHistory();
        search.setRuid(ruid);
        search.setStatus(ParticipantStatusEnum.PROCESS.getStatus());
        if (StringUtils.isNotEmpty(mixParam)) {
            search.setMixParam(mixParam);
        }
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
                partRecObj.addProperty("host",
                        Objects.equals(conferencePartHistory.getUserId(), conference.getUserId()));
                parts.add(partRecObj);
            });
        }

        respObj.add("participants", parts);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }
}
