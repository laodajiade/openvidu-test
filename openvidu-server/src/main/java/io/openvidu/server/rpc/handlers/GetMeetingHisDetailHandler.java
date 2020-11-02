package io.openvidu.server.rpc.handlers;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ParticipantStatusEnum;
import io.openvidu.server.common.pojo.ConferencePartHistory;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Service
public class GetMeetingHisDetailHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String ruid = getStringParam(request, ProtocolElements.GETMEETINGSRECORDDETAIL_RUID_PARAM);
        int pageNum = getIntParam(request, ProtocolElements.PAGENUM);
        int pageSize = getIntParam(request, ProtocolElements.PAGESIZE);
        JsonObject respObj = new JsonObject();
        JsonArray parts = new JsonArray();
        Page<Object> page = PageHelper.startPage(pageNum, pageSize);

        // query part history
        ConferencePartHistory search = new ConferencePartHistory();
        search.setRuid(ruid);
        search.setStatus(ParticipantStatusEnum.LEAVE.getStatus());
        List<ConferencePartHistory> confPartHistoryList = roomManage.getConfHistoryRecordsByCondition(search);
        if (!CollectionUtils.isEmpty(confPartHistoryList)) {
            confPartHistoryList.forEach(conferencePartHistory -> {
                JsonObject partRecObj = new JsonObject();
                partRecObj.addProperty("account", conferencePartHistory.getUuid());
                partRecObj.addProperty("username", conferencePartHistory.getUsername());
                partRecObj.addProperty("userType", conferencePartHistory.getTerminalType());
                partRecObj.addProperty("startTime", conferencePartHistory.getStartTime().getTime());
                partRecObj.addProperty("endTime", conferencePartHistory.getEndTime().getTime());
                partRecObj.addProperty("duration", conferencePartHistory.getDuration());

                parts.add(partRecObj);
            });
        }

        respObj.add("participants", parts);

        respObj.addProperty(ProtocolElements.PAGENUM, pageNum);
        respObj.addProperty(ProtocolElements.PAGESIZE, pageSize);
        respObj.addProperty(ProtocolElements.TOTAL, page.getTotal());
        respObj.addProperty(ProtocolElements.PAGES, page.getPages());
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }
}
