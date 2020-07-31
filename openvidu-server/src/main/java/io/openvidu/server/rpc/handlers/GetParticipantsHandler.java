package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ParticipantSpeakerStatus;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.enums.UserType;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 16:34
 */
@Slf4j
@Service
public class GetParticipantsHandler extends RpcAbstractHandler {
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.GET_PARTICIPANTS_ROOM_ID_PARAM);
        JsonObject respJson = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        List<Long> userIds = new ArrayList<>();

        if (Objects.isNull(sessionManager.getParticipants(sessionId))) {
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
            return;
        }
        sessionManager.getParticipants(sessionId).forEach(s -> {
            if (!Objects.equals(s.getRole(), OpenViduRole.THOR)) {
                userIds.add(Long.valueOf(s.getUserId()));
            }
        });

        if (!CollectionUtils.isEmpty(userIds)) {
            List<User> userList = userMapper.selectByPrimaryKeys(userIds);
            for (User user : userList) {
                KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream().filter(s -> user.getId()
                        .compareTo(Long.valueOf(s.getUserId())) == 0 && Objects.equals(StreamType.MAJOR, s.getStreamType()) &&
                        !Objects.equals(OpenViduRole.THOR, s.getRole())).findFirst().orElse(null);
                if (Objects.isNull(part)) continue;

                // User and dept info.
                UserDeptSearch udSearch = new UserDeptSearch();
                udSearch.setUserId(user.getId());
                UserDept userDeptCom = userDeptMapper.selectBySearchCondition(udSearch);
                if (Objects.isNull(userDeptCom)) {
                    log.warn("GetParticipant userDept is null and privateId:{}, userId:{}",
                            part.getParticipantPrivateId(), user.getId());
                    continue;
                }
                Department userDep = depMapper.selectByPrimaryKey(userDeptCom.getDeptId());
                JsonObject userObj = getPartInfo(part, user, userDep);

                // get device info if have device.
//                String serialNumber = onlineUserList.get(user.getId());
//                if (!StringUtils.isEmpty(serialNumber)) {
//                    log.info("select userId:{} online key(userId):{} serialNumber:{}", user.getId(),
//                            onlineUserList.get(user.getId()), serialNumber);

                    // device and dept info.
                    UserDevice userDeviceSearch = new UserDevice();
                    userDeviceSearch.setUserId(user.getId());
                    UserDevice result = userDeviceMapper.selectByCondition(userDeviceSearch);
                    if (!Objects.isNull(result)) {
                        DeviceSearch deviceSearch = new DeviceSearch();
                        deviceSearch.setSerialNumber(result.getSerialNumber());
                        Device device = deviceMapper.selectBySearchCondition(deviceSearch);
                        DeviceDeptSearch ddSearch = new DeviceDeptSearch();
                        ddSearch.setSerialNumber(result.getSerialNumber());
                        List<DeviceDept> devDeptCom = deviceDeptMapper.selectBySearchCondition(ddSearch);
                        Department devDep = depMapper.selectByPrimaryKey(devDeptCom.get(0).getDeptId());

                        userObj.addProperty("deviceName", device.getDeviceName());
                        userObj.addProperty("deviceOrgName", devDep.getDeptName());
                        userObj.addProperty("appShowName", device.getDeviceName());
                        userObj.addProperty("appShowDesc", devDep.getDeptName());
                    }
//                } else {
//                    userObj.addProperty("appShowName", user.getUsername());
//                    userObj.addProperty("appShowDesc", userDep.getDeptName());
//                }

                jsonArray.add(userObj);
            }
            // add tourist participant
            for (Participant participant : sessionManager.getParticipants(sessionId)) {
                if (StreamType.MAJOR.equals(participant.getStreamType()) && UserType.tourist.equals(participant.getUserType())) {
                    jsonArray.add(getPartInfo(participant, null, null));
                }
            }
        }

        // return the composite order according to session majorShareMixLinkedArr and exclude sharing
        Session session = sessionManager.getSession(sessionId);
        if (ConferenceModeEnum.MCU.equals(session.getConferenceMode())) {
            JsonArray majorShareMixLinkedArr = sessionManager.getSession(sessionId).getMajorShareMixLinkedArr();
            JsonArray orderPartArr = new JsonArray(64);
            JsonArray disorderPartArr = new JsonArray(50);
            List<String> list = new ArrayList<>(16);
            for (JsonElement jsonElement : majorShareMixLinkedArr) {
                JsonObject partObj = jsonElement.getAsJsonObject();
                list.add(partObj.get("connectionId").getAsString());
                for (JsonElement je : jsonArray) {
                    String partConnectId = je.getAsJsonObject().get("connectionId").getAsString();
                    if (partObj.get("connectionId").getAsString().equals(partConnectId)) {
                        orderPartArr.add(je);
                        break;
                    }
                }
            }

            for (JsonElement je : jsonArray) {
                String partConnectId = je.getAsJsonObject().get("connectionId").getAsString();
                if (!list.contains(partConnectId)) {
                    disorderPartArr.add(je);
                }
            }

            orderPartArr.addAll(disorderPartArr);
            jsonArray = orderPartArr;
        }

        respJson.add("participantList", jsonArray);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }

    private static JsonObject getPartInfo(Participant participant, User user, Department department) {
        JsonObject userObj = new JsonObject();
        KurentoParticipant part = (KurentoParticipant) participant;
        userObj.addProperty("connectionId", part.getParticipantPublicId());
        userObj.addProperty("role", part.getRole().name());
        userObj.addProperty("userType", part.getUserType().name());
        userObj.addProperty("terminalType", part.getClientType());
        userObj.addProperty("shareStatus", part.getShareStatus().name());
        userObj.addProperty("handStatus", part.getHandStatus().name());
        // 获取发布者时存在同步阻塞的状态
        userObj.addProperty("audioActive", part.isStreaming() && part.getPublisherMediaOptions().isAudioActive());
        userObj.addProperty("videoActive", part.isStreaming() && part.getPublisherMediaOptions().isVideoActive());
        userObj.addProperty("micStatus", part.getMicStatus().name());
        userObj.addProperty("videoStatus", part.getVideoStatus().name());
        userObj.addProperty("speakerActive", ParticipantSpeakerStatus.on.equals(part.getSpeakerStatus()));

        if (UserType.register.equals(part.getUserType())) {
            userObj.addProperty("userId", user.getId());
            userObj.addProperty("account", user.getUuid());
            userObj.addProperty("username", user.getUsername());
            userObj.addProperty("userOrgName", department.getDeptName());
        } else {
            userObj.addProperty("account", participant.getUuid());
            userObj.addProperty("username", participant.getUsername());
            userObj.addProperty("userId", 0L);
            userObj.addProperty("appShowName", participant.getUsername());
            userObj.addProperty("appShowDesc", "游客");
        }

        return userObj;
    }
}