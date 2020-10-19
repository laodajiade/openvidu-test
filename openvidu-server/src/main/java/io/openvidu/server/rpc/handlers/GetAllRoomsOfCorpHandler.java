package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.PartRoleEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.Role;
import io.openvidu.server.common.pojo.dto.CorpRoomsSearch;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class GetAllRoomsOfCorpHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String roomId = getStringOptionalParam(request, ProtocolElements.GETALLROOMSOFCORP_ROOM_ID);
        JsonArray jsonArray = new JsonArray();
        JsonObject respObj = new JsonObject();
        Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
        Role role = userManage.getUserRoleById(Long.valueOf(userInfo.get("roleId").toString()));
        if (!role.getRoleName().equals(PartRoleEnum.admin.name())) {
            log.warn("userId:{} roleName not admin", userInfo.get("userId"));
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }


        CorpRoomsSearch search = CorpRoomsSearch.builder().project(rpcConnection.getProject())
                .roomIds(roomManage.getSubRoomIds(roomId, getLongOptionalParam(request, "orgId"))).build();
        List<Conference> list = roomManage.getAllRoomsOfCorp(search);
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(conference -> {
                JsonObject jsonObject;
                if (Objects.nonNull(jsonObject = constructConfInfo(conference))) {
                    jsonArray.add(jsonObject);
                }
            });
        }

        respObj.add("roomList", jsonArray);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respObj);
    }

    private JsonObject constructConfInfo(Conference conference) {
        JsonObject jsonObject = null;
        Session session = sessionManager.getSession(conference.getRoomId());
        if (Objects.nonNull(session) && !session.isClosed()) {
            jsonObject = new JsonObject();
            jsonObject.addProperty("roomId", conference.getRoomId());
            jsonObject.addProperty("ruid", conference.getRuid());
            jsonObject.addProperty("password", conference.getPassword());
            jsonObject.addProperty("subject", conference.getConferenceSubject());
            jsonObject.addProperty("conferenceMode", conference.getConferenceMode());
            jsonObject.addProperty("startTime", conference.getStartTime().getTime());
            Participant moderator = session.getModeratorPart();
            if (Objects.nonNull(moderator)) {
                jsonObject.addProperty("moderatorAccount", moderator.getUuid());
                jsonObject.addProperty("moderatorUserId", moderator.getUserId());
                jsonObject.addProperty("moderatorToken", cacheManage.getUserInfoByUUID(moderator.getUuid()).get("token").toString());
            } else {
                return null;
            }
            jsonObject.addProperty("joinNum", session.getParticipants().stream().filter(participant ->
                    StreamType.MAJOR.equals(participant.getStreamType()) && !OpenViduRole.THOR.equals(participant.getRole())).count());
        }

        return jsonObject;
    }
}
