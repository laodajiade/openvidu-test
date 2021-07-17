package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.constants.BrokerChannelConstans;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service(ProtocolElements.URGED_PEOPLE_TO_END_METHOD)
public class UrgedPeopleToEndHandler extends ExRpcAbstractHandler<JsonObject> {


    @Autowired
    private AppointConferenceManage appointConferenceManage;

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        String roomId = BindValidate.notEmptyAndGet(params, "roomId");
        String ruid = BindValidate.notEmptyAndGet(params, "ruid");

        AppointConference appointConference = appointConferenceManage.getByRuid(ruid);
        if (appointConference == null) {
            return RespResult.fail(ErrorCodeEnum.APPOINTMENT_CONFERENCE_NOT_EXIST);
        }

        Conference conference = conferenceMapper.selectUsedConference(appointConference.getRoomId());
        if (conference == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("method", ProtocolElements.URGED_PEOPLE_TO_END_METHOD);

        JsonObject param = new JsonObject();
        jsonObject.add("params", param);
        param.addProperty("roomId", roomId);

        cacheManage.publish(BrokerChannelConstans.TO_OPENVIDU_CHANNEL, jsonObject.toString());
        return RespResult.ok();
    }


    public void notifyToModerator(JsonObject params) {
        String roomId = params.get("roomId").getAsString();
        Session session = sessionManager.getSession(roomId);
        if (session == null) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("ruid", session.getRuid());
        json.addProperty("roomId", roomId);
        json.addProperty("timestamp", System.currentTimeMillis());
        notificationService.sendNotification(session.getModeratorPart().getParticipantPrivateId(),
                ProtocolElements.URGED_PEOPLE_TO_END_NOTIFY_METHOD, json.toString());
    }
}
