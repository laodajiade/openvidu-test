package io.openvidu.server.rpc;

import com.google.gson.*;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.*;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.manage.*;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.common.pojo.SoftUser;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.InviteCompensationManage;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.living.service.LivingManager;
import io.openvidu.server.recording.service.RecordingManager;
import io.openvidu.server.utils.HttpUtil;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author geedow
 * @date 2019/11/5 14:21
 */
@Slf4j
@Component
public abstract class RpcAbstractHandler {

    protected static final Gson gson = new GsonBuilder().create();

    @Resource
    protected OpenviduConfig openviduConfig;

    @Resource
    protected SessionManager sessionManager;
    @Resource
    protected RecordingManager recordingManager;

    @Resource
    protected LivingManager livingManager;

    @Resource
    protected RpcNotificationService notificationService;

    @Resource
    protected CacheManage cacheManage;

    @Resource
    protected DepartmentManage departmentManage;

    @Resource
    protected DeviceManage deviceManage;

    @Resource
    protected UserManage userManage;

    @Resource
    protected DeviceMapper deviceMapper;

    @Resource
    protected ConferenceMapper conferenceMapper;

    @Resource
    protected UserMapper userMapper;

    @Resource
    protected UserDeptMapper userDeptMapper;

    @Resource
    protected DepartmentMapper depMapper;

    @Resource
    protected DeviceDeptMapper deviceDeptMapper;

    @Resource
    protected CorporationMapper corporationMapper;

    @Resource
    protected GroupMapper groupMapper;

    @Resource
    protected UserGroupMapper userGroupMapper;

    @Resource
    protected UserDeviceMapper userDeviceMapper;

    @Resource
    protected ConferenceRecordManage conferenceRecordManage;

    @Resource
    protected ConferenceRecordInfoManage conferenceRecordInfoManage;

    @Resource
    protected RoomManage roomManage;

    @Resource
    protected HttpUtil httpUtil;

    @Resource
    protected PresetMapper presetMapper;

    @Resource
    protected InviteCompensationManage inviteCompensationManage;

    protected void addInviteCompensation(String account, JsonElement jsonElement, String expireTime) {
        inviteCompensationManage.activateInviteCompensation(account, jsonElement, Long.valueOf(expireTime));
    }

    protected void cancelInviteCompensation(String account) {
        inviteCompensationManage.disableInviteCompensation(account);
    }

    protected void cancelAllInviteCompensation(String roomId) {
        inviteCompensationManage.disableAllInviteCompensation(roomId);
    }

    public abstract void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request);

    public static String getStringParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
                    + "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
        }
        return request.getParams().get(key).getAsString();
    }

    public static String getStringOptionalParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            return null;
        }

        return request.getParams().get(key).getAsString();
    }

    public static boolean getBooleanParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
                    + "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
        }
        return request.getParams().get(key).getAsBoolean();
    }

    public static boolean getBooleanOptionalParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            return false;
        }
        return request.getParams().get(key).getAsBoolean();
    }

    protected static long getLongParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
                    + "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
        }
        return request.getParams().get(key).getAsLong();
    }

    protected String leaveRoomAfterConnClosed(String participantPrivateId, EndReason reason) {
        String publicId = null;
        try {
            Participant participant = this.sessionManager.getParticipant(participantPrivateId);
            publicId = Objects.isNull(participant) ? null : participant.getParticipantPublicId();
            sessionManager.evictParticipant(participant, null, null, reason);
            log.info("Evicted participant with privateId {}", participantPrivateId);
        } catch (OpenViduException e) {
            log.warn("Unable to evict: {}", e.getMessage());
            log.trace("Unable to evict user", e);
        }
        return publicId;
    }

    public static Integer getIntOptionalParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            return null;
        }

        return request.getParams().get(key).getAsInt();
    }

    protected static Float getFloatOptionalParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            return null;
        }

        return request.getParams().get(key).getAsFloat();
    }

    protected static List<String> getStringListParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null || !request.getParams().get(key).isJsonArray()) {
            return null;
        }

        List<String> values = new ArrayList<>();
        request.getParams().get(key).getAsJsonArray().forEach(s -> values.add(s.getAsString()));
        return values;
    }

    protected static List<Long> getLongListParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null || !request.getParams().get(key).isJsonArray()) {
            return null;
        }

        List<Long> values = new ArrayList<>();
        request.getParams().get(key).getAsJsonArray().forEach(s -> values.add(s.getAsLong()));
        return values;
    }

    protected static List<JsonObject> getJsonObjectListParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null || !request.getParams().get(key).isJsonArray()) {
            return null;
        }
        List<JsonObject> values = new ArrayList<>();
        request.getParams().get(key).getAsJsonArray().forEach(s -> values.add(s.getAsJsonObject()));
        return values;
    }

    public static int getIntParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
                    + "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
        }
        return request.getParams().get(key).getAsInt();
    }

    public static JsonElement getParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("Request element '" + key + "' is missing in method '" + request.getMethod()
                    + "'. CHECK THAT 'openvidu-server' AND 'openvidu-browser' SHARE THE SAME VERSION NUMBER");
        }
        return request.getParams().get(key);
    }

    public static JsonElement getOptionalParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            return null;
        }
        return request.getParams().get(key);
    }

    protected String generalRoomId() {
        String sessionId = "";
        int tryCnt = 10;
        while (tryCnt-- > 0) {
            sessionId = StringUtil.createSessionId();
            if (isExistingRoom(sessionId, "")) {
                log.warn("conference:{} already exist.", sessionId);
                continue;
            }
            log.info("general sessionId:{}", sessionId);
            break;
        }

        return sessionId;
    }

    protected boolean isExistingRoom(String sessionId, String userUuid) {
        // verify room id ever exists
        ConferenceSearch search = new ConferenceSearch();
        search.setRoomId(sessionId);
        // 会议状态：0 未开始(当前不存在该状态) 1 进行中 2 已结束
        search.setStatus(1);
        AtomicBoolean exitFlag = new AtomicBoolean(false);
        try {
            List<Conference> conferences = conferenceMapper.selectBySearchCondition(search);

            if (conferences != null && !conferences.isEmpty()) {
                /*if (sessionId.equals(userUuid)) {
                    // force close previous room when sessionId is userUuid.
                    log.warn("conference:{} will be force closed.", sessionId);
                    // TODO
                    conferences.forEach(conference -> sessionManager.endConferenceInfo(conference));
                    cleanSession(sessionId, "", false, EndReason.forceCloseSessionByUser);
                    return false;
                }*/
                conferences.forEach(conference -> {
                    Session session = sessionManager.getSession(conference.getRoomId());
                    if (Objects.nonNull(session) && !session.isClosed()) {
                        log.warn("conference:{} already exist.", sessionId);
                        exitFlag.set(true);
                    }
                });
            }
        } catch (Exception e) {
            log.info("exception {}", e);
        }
        return exitFlag.get();
    }

    protected JsonArray deviceList(List<DeviceDept> devices) {
        JsonArray DeviceList = new JsonArray();
        for (DeviceDept device : devices) {
            JsonObject jsonDevice = new JsonObject();
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_SERIAL_NUMBER_PARAM, device.getSerialNumber());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_USER_NAME_PARAM, device.getDeviceName());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_ACCOUNT_PARAM, device.getUuid());
            String status = cacheManage.getDeviceStatus(device.getSerialNumber());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_DEVICESTATUS_PARAM,
                    !StringUtils.isEmpty(status) ? status : DeviceStatus.offline.name());

            DeviceList.add(jsonDevice);
        }
        return  DeviceList;

    }

    protected JsonArray accountList(List<DeviceDept> devices, List<SoftUser> softUsers) {
        JsonArray accountList = new JsonArray();
        for (DeviceDept device : devices) {
            JsonObject jsonDevice = new JsonObject();
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_SERIAL_NUMBER_PARAM, device.getSerialNumber());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_USER_NAME_PARAM, device.getDeviceName());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_ACCOUNT_PARAM, device.getUuid());
            String status = cacheManage.getDeviceStatus(device.getSerialNumber());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_DEVICESTATUS_PARAM,
                    !StringUtils.isEmpty(status) ? status : DeviceStatus.offline.name());

            accountList.add(jsonDevice);
        }
        if (!CollectionUtils.isEmpty(softUsers)) {
            softUsers.stream().forEach(softUser -> {
                JsonObject softUserJson = new JsonObject();
                softUserJson.addProperty(ProtocolElements.GET_SUB_DEVORUSER_SERIAL_NUMBER_PARAM, "");
                softUserJson.addProperty(ProtocolElements.GET_SUB_DEVORUSER_USER_NAME_PARAM, softUser.getUsername());
                softUserJson.addProperty(ProtocolElements.GET_SUB_DEVORUSER_ACCOUNT_PARAM, softUser.getUuid());
                String status = cacheManage.getTerminalStatus(String.valueOf(softUser.getUuid()));
                softUserJson.addProperty(ProtocolElements.GET_SUB_DEVORUSER_DEVICESTATUS_PARAM,
                        !StringUtils.isEmpty(status) ? status : DeviceStatus.offline.name());
                accountList.add(softUserJson);
            });
        }
        return  accountList;

    }

    protected ErrorCodeEnum cleanSession(String sessionId, String privateId, boolean checkModerator, EndReason reason) {
        if (Objects.isNull(sessionManager.getSession(sessionId))) {
            return ErrorCodeEnum.CONFERENCE_NOT_EXIST;
        }

        if (sessionManager.getSession(sessionId).isClosed()) {
            return ErrorCodeEnum.CONFERENCE_ALREADY_CLOSED;
        }

        if (checkModerator && !OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(sessionId, privateId).getRole())) {
            return ErrorCodeEnum.PERMISSION_LIMITED;
        }

        // 1. notify all participant stop publish and receive stream.
        // 2. close session but can not disconnect the connection.
        this.sessionManager.unpublishAllStream(sessionId, reason);
        this.sessionManager.closeSession(sessionId, reason);

        return ErrorCodeEnum.SUCCESS;
    }

    protected static boolean isModerator(OpenViduRole role) {
       return OpenViduRole.MODERATOR_ROLES.contains(role);
    }

    protected Participant sanityCheckOfSession(RpcConnection rpcConnection, StreamType streamType) throws OpenViduException {
        return sessionManager.getParticipant(rpcConnection.getSessionId(), rpcConnection.getParticipantPrivateId(), streamType);
    }

    protected Participant sanityCheckOfSession(RpcConnection rpcConnection, String methodName) throws OpenViduException {
        String participantPrivateId = rpcConnection.getParticipantPrivateId();
        String sessionId = rpcConnection.getSessionId();
        String errorMsg;

        if (sessionId == null) { // null when afterConnectionClosed
            errorMsg = "No session information found for participant with privateId " + participantPrivateId
                    + ". Using the admin method to evict the user.";
            log.warn(errorMsg);
            leaveRoomAfterConnClosed(participantPrivateId, null);
            throw new OpenViduException(OpenViduException.Code.GENERIC_ERROR_CODE, errorMsg);
        } else {
            // Sanity check: don't call RPC method unless the id checks out
            Participant participant = sessionManager.getParticipant(sessionId, participantPrivateId);
            if (participant != null) {
                errorMsg = "Participant " + participant.getParticipantPublicId() + " is calling method '" + methodName
                        + "' in session " + sessionId;
                log.info(errorMsg);
                return participant;
            } else {
                errorMsg = "Participant with private id " + participantPrivateId + " not found in session " + sessionId
                        + ". Using the admin method to evict the user.";
                log.warn(errorMsg);
                leaveRoomAfterConnClosed(participantPrivateId, null);
                throw new OpenViduException(OpenViduException.Code.GENERIC_ERROR_CODE, errorMsg);
            }
        }
    }

    protected Participant sanityCheckOfSession(RpcConnection rpcConnection, String participantPublicId, String methodName) throws OpenViduException {
        String participantPrivateId = rpcConnection.getParticipantPrivateId();
        String sessionId = rpcConnection.getSessionId();
        String errorMsg;

        if (sessionId == null) { // null when afterConnectionClosed
            errorMsg = "No session information found for participant with privateId " + participantPrivateId
                    + ". Using the admin method to evict the user.";
            log.warn(errorMsg);
            leaveRoomAfterConnClosed(participantPrivateId, null);
            throw new OpenViduException(OpenViduException.Code.GENERIC_ERROR_CODE, errorMsg);
        } else {
            // Sanity check: don't call RPC method unless the id checks out
            Participant participant = sessionManager.getParticipantByPrivateAndPublicId(sessionId, participantPrivateId, participantPublicId);
            if (participant != null) {
                errorMsg = "Participant " + participant.getParticipantPublicId() + " is calling method '" + methodName
                        + "' in session " + sessionId;
                log.info(errorMsg);
                return participant;
            } else {
                errorMsg = "Participant with private id " + participantPrivateId + " not found in session " + sessionId
                        + ". Using the admin method to evict the user.";
                log.warn(errorMsg);
                leaveRoomAfterConnClosed(participantPrivateId, null);
                throw new OpenViduException(OpenViduException.Code.GENERIC_ERROR_CODE, errorMsg);
            }
        }
    }

    protected boolean userIsStreamOwner(String sessionId, Participant participant, String streamId) {
        return participant.getParticipantPrivateId()
                .equals(this.sessionManager.getParticipantPrivateIdFromStreamId(sessionId, streamId));
    }

    protected  RpcConnection lookingDevice(RpcConnection rpcConnection, String connectionId){
        io.openvidu.server.core.Session session = this.sessionManager.getSession(rpcConnection.getSessionId());
        Participant participant = session.getParticipantByPublicId(connectionId);
        return notificationService.getRpcConnection(participant.getParticipantPrivateId());
    }

    protected User getUserByRpcConnection(RpcConnection rpcConnection) {
        User user = new User();
        user.setUuid(rpcConnection.getUserUuid());
        user.setUsername("");
        return user;
    }

    protected boolean isAdmin(String uuid) {
        Map userInfo = cacheManage.getUserInfoByUUID(uuid);
        return userInfo.containsKey("role") && "admin".equals(String.valueOf(userInfo.get("role")));
    }
}
