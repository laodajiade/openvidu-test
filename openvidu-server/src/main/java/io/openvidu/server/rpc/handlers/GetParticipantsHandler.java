package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantSpeakerStatus;
import io.openvidu.server.common.enums.UserType;
import io.openvidu.server.common.enums.VoiceMode;
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

import java.util.*;
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
        String searchType = getStringParam(request, "searchType");
        int reducedMode = getIntOptionalParam(request, "reducedMode", 0);
        Set<String> fields = new HashSet<>();

        JsonArray jsonArray = new JsonArray();

        Collection<Participant> needReturnParts = getSearchInstance(searchType).getParts(session, request);


        if (!CollectionUtils.isEmpty(needReturnParts)) {
            Map<String, UserDeviceDeptInfo> connectIdUserInfoMap = getUserInfoInRoom(needReturnParts);

            needReturnParts.forEach(participant -> {
                JsonObject userObj = getPartInfo(participant, connectIdUserInfoMap, rpcConnection);
                if (Objects.nonNull(userObj)) {
                    if (reducedMode == 1) {
                        reducedModeResult(userObj, fields);
                    }

                    jsonArray.add(userObj);
                }
            });
        }

        JsonObject respJson = new JsonObject();
        respJson.add("participantList", jsonArray);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }

    /**
     * 精简返回值
     *
     * @param userObj json obj
     * @param fields  client want to return
     */
    private void reducedModeResult(JsonObject userObj, Set<String> fields) {
        HashSet<String> keys = new HashSet<>(userObj.keySet());
        for (String key : keys) {
            if (!fields.contains(key)) {
                userObj.remove(key);
            }
        }
    }


    public Map<String, UserDeviceDeptInfo> getUserInfoInRoom(Collection<Participant> participants) {
        Map<String, UserDeviceDeptInfo> connectIdPartMap = null;
        List<UserDeviceDeptInfo> userDeviceDeptInfos = userMapper.queryUserInfoByUserIds(participants.stream()
                .map(Participant::getUserId).collect(Collectors.toList()));
        if (!CollectionUtils.isEmpty(userDeviceDeptInfos)) {
            Map<Long, UserDeviceDeptInfo> userIdUserInfoMap = userDeviceDeptInfos.stream()
                    .collect(Collectors.toMap(UserDeviceDeptInfo::getUserId, Function.identity()));
            connectIdPartMap = participants.stream().filter(participant -> userIdUserInfoMap.containsKey(participant.getUserId()))
                    .collect(Collectors.toMap(Participant::getParticipantPublicId,
                            participant -> userIdUserInfoMap.get(participant.getUserId())));
        }
        return connectIdPartMap;
    }


    private Search getSearchInstance(String searchType) {
        switch (searchType) {
            case "exact":
                return new ExactSearch();
            case "list":
                return new ListSearch();
            case "publisher":
                return new PublisherSearch();
            case "raisingHands":
                return new RaisingHandsSearch();
            case "all":
                return new AllSearch();
            default:
                throw new UnsupportedOperationException("unsupported method search " + searchType);
        }
    }


    private static JsonObject getPartInfo(Participant participant, Map<String, UserDeviceDeptInfo> connectIdUserInfoMap, RpcConnection rpcConnection) {
        JsonObject userObj = new JsonObject();
        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        userObj.addProperty("connectionId", kurentoParticipant.getParticipantPublicId());
        userObj.addProperty("role", kurentoParticipant.getRole().name());
        userObj.addProperty("userType", kurentoParticipant.getUserType().name());
        userObj.addProperty("terminalType", kurentoParticipant.getTerminalType().name());
        userObj.addProperty("shareStatus", kurentoParticipant.getShareStatus().name());
        userObj.addProperty("handStatus", kurentoParticipant.getHandStatus().name());

        // todo 2.0 这个放到流中间
        userObj.addProperty("audioActive",
                kurentoParticipant.isStreaming() && kurentoParticipant.getPublisherMediaOptions().isAudioActive());
        userObj.addProperty("videoActive",
                kurentoParticipant.isStreaming() && kurentoParticipant.getPublisherMediaOptions().isVideoActive());


        userObj.addProperty("micStatus", kurentoParticipant.getMicStatus().name());
        userObj.addProperty("videoStatus", kurentoParticipant.getVideoStatus().name());
        userObj.addProperty("speakerStatus", kurentoParticipant.getSpeakerStatus().name());
        userObj.addProperty("speakerActive", ParticipantSpeakerStatus.on.equals(kurentoParticipant.getSpeakerStatus()));
        userObj.addProperty("isVoiceMode", participant.getVoiceMode().equals(VoiceMode.on));
        userObj.addProperty("order", participant.getOrder());
        userObj.addProperty("pushStreamStatus", participant.getPushStreamStatus().name());
        userObj.addProperty("ability", rpcConnection.getAbility());
        userObj.addProperty("functionality", rpcConnection.getFunctionality());
        if (UserType.register.equals(kurentoParticipant.getUserType())) {
            // get user&dept&device from connectIdUserInfoMap
            UserDeviceDeptInfo userDeviceDeptInfo;
            if (Objects.nonNull(userDeviceDeptInfo = connectIdUserInfoMap.get(kurentoParticipant.getParticipantPublicId()))) {
                userObj.addProperty("userId", userDeviceDeptInfo.getUserId());
                userObj.addProperty("account", userDeviceDeptInfo.getUuid());
                userObj.addProperty("username", userDeviceDeptInfo.getUsername());
                userObj.addProperty("userOrgName", userDeviceDeptInfo.getDeptName());
                userObj.addProperty("deviceVersion", userDeviceDeptInfo.getDeviceVersion());
                userObj.addProperty("versionCode", StringUtils.isEmpty(rpcConnection.getDeviceVersion()) ? userDeviceDeptInfo.getDeviceVersion() : rpcConnection.getDeviceVersion());
                if (!StringUtils.isEmpty(userDeviceDeptInfo.getSerialNumber())) {
                    userObj.addProperty("serialNumber", userDeviceDeptInfo.getSerialNumber());
                    userObj.addProperty("deviceModel", userDeviceDeptInfo.getDeviceModel());
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


    /* ************************** class ************************** */
    interface Search {
        Collection<Participant> getParts(Session session, Request<JsonObject> request);
    }

    private class ExactSearch implements Search {

        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            return null;
        }
    }

    private class ListSearch implements Search {
        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            return null;
        }
    }

    private class PublisherSearch implements Search {
        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            return session.getParticipants().stream().filter(p -> p.getRole().needToPublish()).collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private class RaisingHandsSearch implements Search {
        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            return null;
        }
    }

    private class AllSearch implements Search {
        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            return session.getParticipants();
        }
    }

}
