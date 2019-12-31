package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class GetUnfinishedMeetingsHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        boolean isAdmin = getBooleanParam(request, ProtocolElements.GET_UNFINISHED_MEETINGS_ISADMIN_PAPM);
        Conference conference =new Conference();
        conference.setUserId(rpcConnection.getUserId());
        conference.setStatus(1);
        JsonArray jsonArray = new JsonArray();
        List<Conference>  conferenceList = conferenceMapper.selectUnclosedConference(conference);
        if (Objects.isNull(conferenceList)) {
            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonArray);
        }
        for (Conference conference1 : conferenceList) {
            if (Objects.isNull(sessionManager.getSession(conference1.getRoomId())) ||
                    sessionManager.getSession(conference1.getRoomId()).isClosed()) {
                continue;
            }
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(ProtocolElements.GET_UNFINISHED_MEETINGS_ROOMID_PAPM, conference1.getRoomId());
            jsonObject.addProperty(ProtocolElements.GET_UNFINISHED_MEETINGS_SUBJECT_PAPM, conference1.getConferenceSubject());
            jsonObject.addProperty(ProtocolElements.GET_UNFINISHED_MEETINGS_PASSWORD_PAPM, conference1.getPassword());
            jsonObject.addProperty(ProtocolElements.GET_UNFINISHED_MEETINGS_ROOMCREAEAT_PAPM, conference1.getCreateTime().getTime());
            User user = userMapper.selectByPrimaryKey(conference1.getUserId());
            jsonObject.addProperty(ProtocolElements.GET_UNFINISHED_MEETINGS_ACCOUNT_PAPM, user.getUuid());
            jsonArray.add(jsonObject);
        }

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonArray);
    }
}
