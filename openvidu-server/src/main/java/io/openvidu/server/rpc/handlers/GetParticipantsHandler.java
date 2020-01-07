package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.enums.UserOnlineStatusEnum;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.kurento.core.KurentoParticipant;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

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

        Map<Long, String> onlineUserList = new HashMap<>();
        for (RpcConnection c : notificationService.getRpcConnections()) {
            if (Objects.equals(AccessTypeEnum.web, c.getAccessType())) continue;
            Map userInfo = cacheManage.getUserInfoByUUID(c.getUserUuid());
            if (Objects.isNull(userInfo)) continue;
            String status = String.valueOf(userInfo.get("status"));
            if (Objects.equals(UserOnlineStatusEnum.online.name(), status)) {
                onlineUserList.put(c.getUserId(), c.getSerialNumber());
                log.info("Status:{}, privateId:{}, userId:{}, serialNumber:{}", status, c.getParticipantPrivateId(), c.getUserId(), c.getSerialNumber());
            }
        }

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
            userList.forEach(user -> {
                KurentoParticipant part = (KurentoParticipant) sessionManager.getParticipants(sessionId).stream().filter(s -> user.getId()
                        .compareTo(Long.valueOf(s.getUserId())) == 0 && Objects.equals(StreamType.MAJOR, s.getStreamType()) &&
                        !Objects.equals(OpenViduRole.THOR, s.getRole())).findFirst().orElse(null);
                if (Objects.isNull(part)) return;

                // User and dept info.
                UserDeptSearch udSearch = new UserDeptSearch();
                udSearch.setUserId(user.getId());
                UserDept userDeptCom = userDeptMapper.selectBySearchCondition(udSearch);
                if (Objects.isNull(userDeptCom)) {
                    log.warn("GetParticipant userDept is null and privateId:{}, userId:{}",
                            part.getParticipantPrivateId(), user.getId());
                    return;
                }
                Department userDep = depMapper.selectByPrimaryKey(userDeptCom.getDeptId());

                JsonObject userObj = new JsonObject();
                userObj.addProperty("userId", user.getId());
                userObj.addProperty("account", user.getUsername());
                userObj.addProperty("userOrgName", userDep.getDeptName());
                userObj.addProperty("role", part.getRole().name());
                userObj.addProperty("shareStatus", part.getShareStatus().name());
                userObj.addProperty("handStatus", part.getHandStatus().name());
                // 获取发布者时存在同步阻塞的状态
                userObj.addProperty("audioActive", !part.isStreaming() || part.getPublisherMediaOptions().isAudioActive());
                userObj.addProperty("videoActive", !part.isStreaming() || part.getPublisherMediaOptions().isVideoActive());

                // get device info if have device.
                String serialNumber = onlineUserList.get(user.getId());
                if (!StringUtils.isEmpty(serialNumber)) {
                    log.info("select userId:{} online key(userId):{} serialNumber:{}", user.getId(),
                            onlineUserList.get(user.getId()), serialNumber);

                    // device and dept info.
                    DeviceSearch deviceSearch = new DeviceSearch();
                    deviceSearch.setSerialNumber(serialNumber);
                    Device device = deviceMapper.selectBySearchCondition(deviceSearch);
                    DeviceDeptSearch ddSearch = new DeviceDeptSearch();
                    ddSearch.setSerialNumber(serialNumber);
                    List<DeviceDept> devDeptCom = deviceDeptMapper.selectBySearchCondition(ddSearch);
                    Department devDep = depMapper.selectByPrimaryKey(devDeptCom.get(0).getDeptId());

                    userObj.addProperty("deviceName", device.getDeviceName());
                    userObj.addProperty("deviceOrgName", devDep.getDeptName());
                    userObj.addProperty("appShowName",  device.getDeviceName());
                    userObj.addProperty("appShowDesc", "(" + device.getDeviceModel() + ") " + devDep.getDeptName());
                } else {
                    userObj.addProperty("appShowName", user.getUsername());
                    userObj.addProperty("appShowDesc", "(" + user.getTitle() + ") " + userDep.getDeptName());
                }

                jsonArray.add(userObj);
            });
        }
        respJson.add("participantList", jsonArray);
        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}
