package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 20:35
 */
@Slf4j
@Service
public class GetNotFinishedRoomHandler extends RpcAbstractHandler {

    @Autowired
    RestTemplate restTemplate;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        JsonObject respObj = getInfo(rpcConnection.getUserUuid(), true);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }

    public JsonObject getInfo(String uuid, boolean requestOtherServer) {
        JsonObject respObj = new JsonObject();
        Map partInfo = cacheManage.getPartInfo(uuid);
        if (!partInfo.isEmpty()) {
            String roomId = String.valueOf(partInfo.get("roomId"));
            Map roomInfo = cacheManage.getRoomInfo(roomId);
            Session session = sessionManager.getSession(roomId);
            // 临时处理，去其他服务器上获取信息
            if (!roomInfo.isEmpty() && Objects.isNull(session) && requestOtherServer) {
                String instanceId = roomInfo.get("instanceId").toString();
                String[] arr = instanceId.split(":");
                String url = "http://" + arr[1] + ":" + arr[2] + "/inner/getNotFinishRoom?uuid=" + uuid;
                String result = restTemplate.getForObject(url, String.class);
                return gson.fromJson(result, JsonObject.class);
            }

            if (!roomInfo.isEmpty() && Objects.nonNull(session) && !session.isClosed() && !session.isClosing()) {
                // room info
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_ID_PARAM, roomId);
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_SUBJECT_PARAM, String.valueOf(session.getPresetInfo().getRoomSubject()));
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_PASSWORD_PARAM, String.valueOf(roomInfo.get("password")));

                // participant info.
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_ROLE_PARAM, String.valueOf(partInfo.get("role")));
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_AUDIOACTIVE_PARAM, partInfo.get("micStatus").toString());
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_VIDEOACTIVE_PARAM, partInfo.get("videoStatus").toString());
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_SPEAKERSTATUS_PARAM, String.valueOf(partInfo.get("speakerStatus")));
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_HANDSTATUS_PARAM, String.valueOf(partInfo.get("handStatus")));
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_SHARESTATUS_PARAM, String.valueOf(partInfo.get("shareStatus")));
                respObj.addProperty(ProtocolElements.GET_NOT_FINISHED_ROOM_MODERATOR_UUID_PARAM, session.getConference().getModeratorUuid());
            }
        }
        return respObj;
    }
}
