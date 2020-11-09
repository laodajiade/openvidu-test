package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 20:35
 */
@Slf4j
@Service
public class GetNotFinishedRoomHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        JsonObject respObj = new JsonObject();
        Map partInfo = cacheManage.getPartInfo(rpcConnection.getUserUuid());
        if (!partInfo.isEmpty()) {
            String roomId = String.valueOf(partInfo.get("roomId"));
            Map roomInfo = cacheManage.getRoomInfo(roomId);
            Session session = sessionManager.getSession(roomId);
            if (!roomInfo.isEmpty() && Objects.nonNull(session) && !session.isClosed()) {
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

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }
}
