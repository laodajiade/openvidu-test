package io.openvidu.server.rpc;

import cn.jpush.api.push.model.notification.IosAlert;
import com.google.gson.*;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.*;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.common.enums.TerminalTypeEnum;
import io.openvidu.server.common.manage.*;
import io.openvidu.server.common.pojo.CallHistory;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.common.pojo.SoftUser;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.vo.CallHistoryVo;
import io.openvidu.server.config.EnvConfig;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.InviteCompensationManage;
import io.openvidu.server.core.JpushManage;
import io.openvidu.server.core.JpushMsgEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.core.TimerManager;
import io.openvidu.server.living.service.LivingManager;
import io.openvidu.server.recording.service.RecordingManager;
import io.openvidu.server.utils.HttpUtil;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author geedow
 * @date 2019/11/5 14:21
 */
@Slf4j
@Component
public abstract class RpcAbstractHandler {

    protected static final Gson gson = new GsonBuilder().create();
    @Autowired
    protected EnvConfig envConfig;

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
    protected DepartmentMapper departmentMapper;

    @Resource
    protected HttpUtil httpUtil;

    @Resource
    protected PresetMapper presetMapper;

    @Resource
    protected ConferencePartHistoryMapper conferencePartHistoryMapper;

    @Resource
    protected RoleMapper roleMapper;

    @Resource
    protected JpushMessageMapper jpushMessageMapper;

    @Resource
    protected CallHistoryMapper callHistoryMapper;

    @Resource
    protected InviteCompensationManage inviteCompensationManage;

    @Resource
    protected TimerManager timerManager;

    @Autowired
    protected AppointConferenceManage appointConferenceManage;

    @Autowired
    protected StatisticsManage statisticsManage;

    @Resource
    protected JpushManage jpushManage;

    protected BigDecimal bigDecimalMB = new BigDecimal(1024 * 1024);

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

    public static String getStringOptionalParam(Request<JsonObject> request, String key,String defaultValue) {
        if (request.getParams() == null || request.getParams().get(key) == null || "".equals(request.getParams().get(key).getAsString())) {
            return defaultValue;
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

    public static Long getLongOptionalParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            return null;
        }

        return request.getParams().get(key).getAsLong();
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

    protected boolean isExistingRoom(String sessionId) {
        // verify room id ever exists
        ConferenceSearch search = new ConferenceSearch();
        search.setRoomId(sessionId);
        // 会议状态：0 未开始(当前不存在该状态) 1 进行中 2 已结束
        search.setStatus(1);
        AtomicBoolean exitFlag = new AtomicBoolean(false);
        try {
            List<Conference> conferences = conferenceMapper.selectBySearchCondition(search);

            if (conferences != null && !conferences.isEmpty()) {
                conferences.forEach(conference -> {
                    Session session = sessionManager.getSession(conference.getRoomId());
                    if (Objects.nonNull(session) && !session.isClosed()) {
                        log.warn("conference:{} already exist.", session.getSessionId());
                        exitFlag.set(true);
                    }
                });
            }
        } catch (Exception e) {
            log.info("isExistingRoom ", e);
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
                String status = cacheManage.getTerminalStatus(softUser.getUuid());
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

    protected boolean isReadWrite(String uuid) {
        Map userInfo = cacheManage.getUserInfoByUUID(uuid);
        return userInfo.containsKey("readWrite") && "1".equals(String.valueOf(userInfo.get("readWrite")));
    }

    public RpcNotificationService getNotificationService() {
        return notificationService;
    }

    public void setNotificationService(RpcNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    protected void saveCallHistoryUsers(List<User> invitees, String sessionId, String ruid) {
        List<CallHistoryVo> callHistories = callHistoryMapper.getCallHistoryList(ruid);
        List<CallHistory> addList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(invitees)) {
            if (!CollectionUtils.isEmpty(callHistories)) {
                List<String> list = callHistories.stream().map(CallHistoryVo::getUuid).collect(Collectors.toList());
                invitees.forEach(invitee -> {
                    if (!list.contains(invitee.getUuid())) {
                        CallHistory callHistory = new CallHistory();
                        callHistory.setRoomId(sessionId);
                        callHistory.setUuid(invitee.getUuid());
                        callHistory.setUsername(invitee.getUsername());
                        callHistory.setRuid(ruid);
                        addList.add(callHistory);
                    }
                });
            } else {
                invitees.forEach(invitee -> {
                    CallHistory callHistory = new CallHistory();
                    callHistory.setRoomId(sessionId);
                    callHistory.setUuid(invitee.getUuid());
                    callHistory.setUsername(invitee.getUsername());
                    callHistory.setRuid(ruid);
                    addList.add(callHistory);
                });
            }
        }

        if (!CollectionUtils.isEmpty(addList)) {
            callHistoryMapper.insertBatch(addList);
        }
    }

    @Async
    public void sendJpushMessage(List<String> targetIds, String moderatorName, String subject, String ruid) {
        targetIds.forEach(uuid ->{
            Map userInfo = cacheManage.getUserInfoByUUID(uuid);
            if (Objects.nonNull(userInfo) && !userInfo.isEmpty()) {
                if (Objects.nonNull(userInfo.get("type")) && Objects.nonNull(userInfo.get("registrationId"))) {
                    String type = userInfo.get("type").toString();
                    String registrationId = userInfo.get("registrationId").toString();
                    Date createDate = new Date();
                    String title = StringUtil.INVITE_CONT;
                    String alert = String.format(StringUtil.ON_MEETING_INVITE, moderatorName, subject);
                    Map<String,String> map = new HashMap<>(1);
                    map.put("message", jpushManage.getJpushMsgTemp(ruid, title, alert, createDate, JpushMsgEnum.MEETING_INVITE.getMessage()));
                    if (TerminalTypeEnum.A.name().equals(type)) {
                        jpushManage.sendToAndroid(title, alert, map, registrationId);
                    } else if(TerminalTypeEnum.I.name().equals(type)){
                        IosAlert iosAlert = IosAlert.newBuilder().setTitleAndBody(title, null, alert).build();
                        jpushManage.sendToIos(iosAlert, map, registrationId);
                    }
                    jpushManage.saveJpushMsg(uuid, ruid, JpushMsgEnum.MEETING_INVITE.getMessage(), alert, createDate);
                }
            }
        });
    }
}
