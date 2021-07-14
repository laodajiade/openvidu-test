package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.ParticipantHandStatus;
import io.openvidu.server.common.enums.UserType;
import io.openvidu.server.common.enums.VoiceMode;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
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

        JsonArray jsonArray = new JsonArray();

        Collection<Participant> needReturnParts = getSearchInstance(searchType).getParts(session, request);


        if (!CollectionUtils.isEmpty(needReturnParts)) {
            Map<String, UserDeviceDeptInfo> connectIdUserInfoMap = getUserInfoInRoom(needReturnParts);

            needReturnParts.forEach(participant -> {
                JsonObject userObj = getPartInfo(participant, connectIdUserInfoMap, rpcConnection);
                if (Objects.nonNull(userObj)) {
                    if (reducedMode == 1) {
                        Set<String> fields = new HashSet<>(getStringListParam(request, "fields"));
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


    private JsonObject getPartInfo(Participant participant, Map<String, UserDeviceDeptInfo> connectIdUserInfoMap, RpcConnection rpcConnection) {
        JsonObject userObj = new JsonObject();
        KurentoParticipant kurentoParticipant = (KurentoParticipant) participant;
        userObj.addProperty("connectionId", kurentoParticipant.getParticipantPublicId());
        userObj.addProperty("role", kurentoParticipant.getRole().name());
        userObj.addProperty("userType", kurentoParticipant.getUserType().name());
        userObj.addProperty("terminalType", kurentoParticipant.getTerminalType().name());
        userObj.addProperty("shareStatus", kurentoParticipant.getShareStatus().name());
        userObj.addProperty("handStatus", kurentoParticipant.getHandStatus().name());
        userObj.addProperty("micStatus", kurentoParticipant.getMicStatus().name());
        userObj.addProperty("videoStatus", kurentoParticipant.getVideoStatus().name());
        userObj.addProperty("speakerStatus", kurentoParticipant.getSpeakerStatus().name());
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
        userObj.add("streams", this.getStreams(kurentoParticipant));

        return userObj;
    }


    public JsonArray getStreams(KurentoParticipant kParticipant) {
        JsonArray streamsArray = new JsonArray();
        for (PublisherEndpoint publisher : kParticipant.getPublishers().values()) {
            try {
                JsonObject stream = new JsonObject();
                stream.addProperty(ProtocolElements.JOINROOM_PEERPUBLISHID_PARAM,
                        publisher.getStreamId());
                stream.addProperty(ProtocolElements.JOINROOM_PEERCREATEDAT_PARAM,
                        publisher.createdAt());
                stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMHASAUDIO_PARAM,
                        publisher.getMediaOptions().hasAudio);
                stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMHASVIDEO_PARAM,
                        publisher.getMediaOptions().hasVideo);
                stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMMIXINCLUDED_PARAM,
                        kParticipant.isMixIncluded());
                stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMTYPEOFVIDEO_PARAM,
                        publisher.getMediaOptions().typeOfVideo);
                stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMFRAMERATE_PARAM,
                        publisher.getMediaOptions().frameRate);
                stream.addProperty(ProtocolElements.JOINROOM_PEERSTREAMVIDEODIMENSIONS_PARAM,
                        publisher.getMediaOptions().videoDimensions);

                stream.addProperty("StreamType",
                        publisher.getStreamType().name());
                JsonElement filter = publisher.getMediaOptions().getFilter() != null
                        ? publisher.getMediaOptions().getFilter().toJson()
                        : new JsonObject();
                stream.add(ProtocolElements.JOINROOM_PEERSTREAMFILTER_PARAM, filter);

                streamsArray.add(stream);
            } catch (Exception e) {
                log.error("get participant stream {} error", publisher.getStreamId(), e);
            }

        }
        return streamsArray;
    }

    /* ************************** inner class ************************** */
    interface Search {
        Collection<Participant> getParts(Session session, Request<JsonObject> request);
    }

    private class ExactSearch implements Search {

        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            List<String> targetIds = getStringListParam(request, "targetIds");
            return session.getParticipants().stream().filter(item -> targetIds.contains(item.getUuid())).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private class ListSearch implements Search {
        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            int order = getIntParam(request, "order");
            int reverse = getIntParam(request, "reverse");
            int limit = getIntParam(request, "limit");
            if (reverse == 1) {
                return session.getParticipants().stream().filter(p -> p.getOrder() > order).limit(limit).sorted(Comparator.comparing(Participant::getOrder)).collect(Collectors.toCollection(LinkedHashSet::new));
            } else {
                return session.getParticipants().stream().filter(p -> p.getOrder() > order).limit(limit).sorted(Comparator.comparing(Participant::getOrder).reversed()).collect(Collectors.toCollection(LinkedHashSet::new));
            }
        }
    }


    private class PublisherSearch implements Search {
        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            return session.getParticipants().stream().filter(p -> p.getRole().needToPublish()).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private class RaisingHandsSearch implements Search {
        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            return session.getParticipants().stream().filter(p -> p.getHandStatus().equals(ParticipantHandStatus.up)).collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private class AllSearch implements Search {
        @Override
        public Set<Participant> getParts(Session session, Request<JsonObject> request) {
            return session.getParticipants();
        }
    }
    /* ************************** inner class ************************** */
}
