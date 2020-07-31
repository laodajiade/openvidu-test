package io.openvidu.server.rpc;

import com.google.gson.*;
import io.openvidu.client.OpenViduException;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.*;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.*;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.ConferenceSearch;
import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.core.EndReason;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.kurento.core.KurentoSession;
import io.openvidu.server.living.service.LivingManager;
import io.openvidu.server.recording.service.RecordingManager;
import io.openvidu.server.utils.HttpUtil;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

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

    public static int getIntParam(Request<JsonObject> request, String key) {
        if (request.getParams() == null || request.getParams().get(key) == null) {
            throw new RuntimeException("RMBER");
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
        try {
            List<Conference> conferences = conferenceMapper.selectBySearchCondition(search);
            if (conferences != null && !conferences.isEmpty()) {
                if (sessionId.equals(userUuid)) {
                    // force close previous room when sessionId is userUuid.
                    log.warn("conference:{} will be force closed.", sessionId);
                    // TODO
                    conferences.forEach(conference -> sessionManager.endConferenceInfo(conference));
                    cleanSession(sessionId, "", false, EndReason.forceCloseSessionByUser);
                    return false;
                }

                log.warn("conference:{} already exist.", sessionId);
                return true;
            }
        } catch (Exception e) {
            log.info("exception {}", e);
        }
        return false;
    }

    protected JsonArray deviceList(List<DeviceDept> devices) {
        JsonArray DeviceList = new JsonArray();
        for (DeviceDept device : devices) {
            JsonObject jsonDevice = new JsonObject();
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_SERIAL_NUMBER_PARAM, device.getSerialNumber());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_DEVICE_NAME_PARAM, device.getDeviceName());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_ACCOUNT_PARAM, device.getUuid());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_USERID_PARAM, device.getUserId());
            String status = cacheManage.getDeviceStatus(device.getSerialNumber());
            jsonDevice.addProperty(ProtocolElements.GET_SUB_DEVORUSER_DEVICESTATUS_PARAM,
                    !StringUtils.isEmpty(status) ? status : DeviceStatus.offline.name());

            DeviceList.add(jsonDevice);
        }
        return  DeviceList;

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

    protected boolean updateReconnectInfo(RpcConnection rpcConnection) {
        RpcConnection oldRpcConnection = null;
        String oldPrivateId = null;
        try {
            Session session;
            Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
            if (Objects.isNull(userInfo)) {
                log.warn("user:{} info is null.", rpcConnection.getUserUuid());
                return false;
            }

            if (Objects.equals(UserOnlineStatusEnum.reconnect.name(), userInfo.get("status"))) {
                log.info("reconnect userId:{} mac:{}", rpcConnection.getUserId(), rpcConnection.getMacAddr());
                oldPrivateId = String.valueOf(userInfo.get("reconnect"));
                if (StringUtils.isEmpty(oldPrivateId)) {
                    log.warn("reconnect privateId:{}", oldPrivateId);
                    return false;
                }

                oldRpcConnection = notificationService.getRpcConnection(oldPrivateId);
                cacheManage.updateTerminalStatus(rpcConnection.getUserUuid(), UserOnlineStatusEnum.online,
                        rpcConnection.getSerialNumber(), DeviceStatus.online);
                cacheManage.updateReconnectInfo(rpcConnection.getUserUuid(), "");
                if (StringUtils.isEmpty(rpcConnection.getSessionId()) || Objects.isNull(session =
                        this.sessionManager.getSession(rpcConnection.getSessionId()))) {
                    notificationService.closeRpcSession(oldPrivateId);
                    return false;
                }
                Participant participant = this.sessionManager.getParticipant(rpcConnection.getSessionId(),
                        oldPrivateId, StreamType.MAJOR);
                if (Objects.equals(ConferenceModeEnum.SFU, session.getConferenceMode()) && Objects.equals(OpenViduRole.MODERATOR,
                        participant.getRole()) && Objects.equals(StreamType.MAJOR, participant.getStreamType())) {
                    sessionManager.getSession(participant.getSessionId()).setModeratorIndex(participant.getParticipantPublicId());
                }
                String partPublicId = leaveRoomAfterConnClosed(oldPrivateId, EndReason.sessionClosedByServer);
                // update partLinkedArr and sharing status in conference
                Participant speakPart = this.sessionManager.getSpeakerPart(rpcConnection.getSessionId());
                if (Objects.isNull(speakPart)) {
                    Participant moderatorPart = this.sessionManager.getModeratorPart(rpcConnection.getSessionId());
                    if (Objects.nonNull(moderatorPart)) {
                        session.reorder(moderatorPart.getParticipantPublicId());
                    }
                }
                session.evictReconnectOldPart(partPublicId);
                Participant sharingPart = this.sessionManager.getParticipant(rpcConnection.getSessionId(),
                        oldPrivateId, StreamType.SHARING);
                if (!Objects.isNull(sharingPart)) {
                    session.evictReconnectOldPart(sharingPart.getParticipantPublicId());
                    KurentoSession kurentoSession = (KurentoSession) session;
                    if (!StringUtils.isEmpty(kurentoSession.compositeService.getShareStreamId()) &&
                            kurentoSession.compositeService.getShareStreamId().contains(sharingPart.getParticipantPublicId())) {
                        kurentoSession.compositeService.setShareStreamId(null);
                        kurentoSession.compositeService.setExistSharing(false);
                    }
                    sessionManager.evictParticipant(sharingPart, null, null, EndReason.sessionClosedByServer);
                }
                sessionManager.accessOut(oldRpcConnection);
                return true;
            }
        } catch (Exception e) {
            log.warn("exception:{}", e);
            if (!StringUtils.isEmpty(oldPrivateId))
                notificationService.closeRpcSession(oldPrivateId);
            return false;
        }

        return true;
    }

    protected Participant sanityCheckOfSession(RpcConnection rpcConnection, StreamType streamType) throws OpenViduException {
        Participant participant = sessionManager.getParticipant(rpcConnection.getSessionId(),
                rpcConnection.getParticipantPrivateId(), streamType);
        if (participant == null) {
            leaveRoomAfterConnClosed(rpcConnection.getParticipantPrivateId(), null);
            throw new OpenViduException(OpenViduException.Code.GENERIC_ERROR_CODE, "Participant not exists.");
        }
        return participant;
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