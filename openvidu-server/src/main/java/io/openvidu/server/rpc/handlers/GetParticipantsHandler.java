package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author geedow
 * @date 2019/11/5 16:34
 */
@Slf4j
@Service
public class GetParticipantsHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        Session session;
        if (Objects.isNull(session = sessionManager.getSession(getStringParam(request,
                ProtocolElements.GET_PARTICIPANTS_ROOM_ID_PARAM)))) {
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(),
                    ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED);
            return;
        }
        String targetId = getStringOptionalParam(request,ProtocolElements.GET_PARTICIPANTS_TARGETID_PARAM);
        JsonArray jsonArray = new JsonArray();
        // key:connectionId, value:userDeviceDeptInfo
        Map<String, UserDeviceDeptInfo> connectIdUserInfoMap;
        Set<Participant> needReturnParts = session.getMajorPartAllOrSpecificConnect(targetId);
        JsonArray majorShareMixLinkedArr = session.getMajorShareMixLinkedArr();
        if (!CollectionUtils.isEmpty(needReturnParts)
                && Objects.nonNull(connectIdUserInfoMap = userManage.getUserInfoInRoom(needReturnParts))
                && !connectIdUserInfoMap.isEmpty()) {
            // key:connectionId, value:participant
            Map<String, Participant> connectIdPartMap = needReturnParts.stream()
                    .collect(Collectors.toMap(Participant::getParticipantPublicId, Function.identity()));

            // add participant info one by one according to mcu mix order
            for (JsonElement jsonElement : majorShareMixLinkedArr) {
                Participant participant;
                JsonObject connectIdStreamTypeObj = jsonElement.getAsJsonObject();
                if (StreamType.MAJOR.name().equals(connectIdStreamTypeObj.get("streamType").getAsString())
                        && Objects.nonNull(participant = connectIdPartMap.remove(connectIdStreamTypeObj.get("connectionId").getAsString()))) {
                    JsonObject userObj = getPartInfo(participant, connectIdUserInfoMap);
                    if (Objects.nonNull(userObj)) {
                        jsonArray.add(userObj);
                    }
                }
            }

            // add left participant in return info
            if (!connectIdPartMap.isEmpty()) {
                Collection<Participant> remainParts = connectIdPartMap.values();
                for (Participant participant : remainParts) {
                    JsonObject userObj = getPartInfo(participant, connectIdUserInfoMap);
                    if (Objects.nonNull(userObj)) {
                        jsonArray.add(userObj);
                    }
                }
            }
        }

        JsonObject respJson = new JsonObject();
        respJson.add("participantList", jsonArray);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }

    private static JsonObject getPartInfo(Participant participant, Map<String, UserDeviceDeptInfo> connectIdUserInfoMap) {
        JsonObject userObj = new JsonObject();
        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        userObj.addProperty("connectionId", kurentoParticipant.getParticipantPublicId());
        userObj.addProperty("role", kurentoParticipant.getRole().name());
        userObj.addProperty("userType", kurentoParticipant.getUserType().name());
        userObj.addProperty("terminalType", kurentoParticipant.getTerminalType().name());
        userObj.addProperty("shareStatus", kurentoParticipant.getShareStatus().name());
        userObj.addProperty("handStatus", kurentoParticipant.getHandStatus().name());
        userObj.addProperty("audioActive",
                kurentoParticipant.isStreaming() && kurentoParticipant.getPublisherMediaOptions().isAudioActive());
        userObj.addProperty("videoActive",
                kurentoParticipant.isStreaming() && kurentoParticipant.getPublisherMediaOptions().isVideoActive());
        userObj.addProperty("micStatus", kurentoParticipant.getMicStatus().name());
        userObj.addProperty("videoStatus", kurentoParticipant.getVideoStatus().name());
        userObj.addProperty("speakerActive", ParticipantSpeakerStatus.on.equals(kurentoParticipant.getSpeakerStatus()));
        userObj.addProperty("isVoiceMode", participant.getVoiceMode().equals(VoiceMode.on));

        if (UserType.register.equals(kurentoParticipant.getUserType())) {
            // get user&dept&device from connectIdUserInfoMap
            UserDeviceDeptInfo userDeviceDeptInfo;
            if (Objects.nonNull(userDeviceDeptInfo = connectIdUserInfoMap.get(kurentoParticipant.getParticipantPublicId()))) {
                userObj.addProperty("userId", userDeviceDeptInfo.getUserId());
                userObj.addProperty("account", userDeviceDeptInfo.getUuid());
                userObj.addProperty("username", userDeviceDeptInfo.getUsername());
                userObj.addProperty("userOrgName", userDeviceDeptInfo.getDeptName());
                if (!StringUtils.isEmpty(userDeviceDeptInfo.getSerialNumber())) {
                    userObj.addProperty("deviceName", userDeviceDeptInfo.getDeviceName());
                    userObj.addProperty("deviceOrgName", userDeviceDeptInfo.getDeptName());
                    userObj.addProperty("appShowName", userDeviceDeptInfo.getDeviceName());
                    userObj.addProperty("appShowDesc", userDeviceDeptInfo.getDeptName());
                }
            } else {
                return null;
            }
        } else {    //  tourist
            userObj.addProperty("account", participant.getUuid());
            userObj.addProperty("username", participant.getUsername());
            userObj.addProperty("userId", 0L);
            userObj.addProperty("appShowName", participant.getUsername());
            userObj.addProperty("appShowDesc", "游客");
        }

        return userObj;
    }

}
