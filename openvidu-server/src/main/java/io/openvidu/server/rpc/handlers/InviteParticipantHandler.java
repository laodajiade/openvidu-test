package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author geedow
 * @date 2019/11/5 20:02
 */
@Slf4j
@Service
public class InviteParticipantHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringParam(request, ProtocolElements.INVITE_PARTICIPANT_ID_PARAM);
        String sourceId = getStringParam(request, ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM);
        String expireTime = getStringParam(request, ProtocolElements.INVITE_PARTICIPANT_EXPIRETIME_PARAM);
        List<String> targetIds = getStringListParam(request, ProtocolElements.INVITE_PARTICIPANT_TARGET_ID_PARAM);
        if (CollectionUtils.isEmpty(targetIds)) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        if (!OpenViduRole.MODERATOR_ROLES.contains(sessionManager.getParticipant(sessionId,
                rpcConnection.getParticipantPrivateId()).getRole())) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.PERMISSION_LIMITED);
            return;
        }

        SessionPreset preset = sessionManager.getPresetInfo(sessionId);
        if (!Objects.isNull(sessionManager.getSession(sessionId))) {
            Set<Participant> majorParts = sessionManager.getSession(sessionId).getMajorPartEachConnect();
            if ((majorParts.size() + targetIds.size()) > preset.getRoomCapacity()) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.ROOM_CAPACITY_PERSONAL_LIMITED);
                return;
            }
        }

        //判断发起会议时是否超出企业人数上限
        Collection<Session> sessions = sessionManager.getSessions();
        if (Objects.nonNull(sessions)) {
            AtomicInteger limitCapacity = new AtomicInteger();
            sessions.forEach(e ->{
                if (rpcConnection.getProject().equals(e.getConference().getProject())) {
                    limitCapacity.addAndGet(e.getMajorPartEachConnect().size());
                }
            });
            //query sd_corporation info
            Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
            if (Objects.nonNull(corporation.getCapacity()) && targetIds.size() + limitCapacity.get() > corporation.getCapacity()) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.ROOM_CAPACITY_CORP_LIMITED);
                return ;
            }
        }

        Map userInfo = cacheManage.getUserInfoByUUID(rpcConnection.getUserUuid());
        Object username = userInfo.get("username");
        String deviceName = userInfo.containsKey("deviceName") ? String.valueOf(userInfo.get("deviceName")) : null;
        String userIcon = userInfo.containsKey("userIcon") ? String.valueOf(userInfo.get("userIcon")) : "";
        String moderatorName = Objects.isNull(username) ? deviceName : username.toString();
        // find the target rpc connection by targetId list and notify info.
        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_ID_PARAM, sessionId);
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM, sourceId);
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERNAME_PARAM, moderatorName);
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERICON_PARAM, userIcon);
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_EXPIRETIME_PARAM, expireTime);
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SUBJECT_PARAM, preset.getRoomSubject());
        if (!StringUtils.isEmpty(deviceName)) {
            params.addProperty(ProtocolElements.INVITE_PARTICIPANT_DEVICE_NAME_PARAM, deviceName);
        }

        List<User> invitees = userMapper.selectCallUserByUuidList(targetIds);
        if (!CollectionUtils.isEmpty(invitees)) {
            invitees.forEach(invitee -> {
                cacheManage.saveInviteInfo(sessionId,invitee.getUuid());
                addInviteCompensation(invitee.getUuid(), params, expireTime);
            });
        }
        inviteOnline(targetIds, params);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());

        Conference conference = sessionManager.getSession(sessionId).getConference();
        saveCallHistoryUsers(invitees, sessionId, conference.getRuid());
        //极光推送
        sendJpushMessage(targetIds, moderatorName, conference.getConferenceSubject(), conference.getRuid());

    }


    public void inviteOnline(List<String> targetIds, JsonObject params) {
        Collection<RpcConnection> rpcConnections = this.notificationService.getRpcConnections();
        for (RpcConnection rpcConnect : rpcConnections) {
            if (!Objects.isNull(rpcConnect.getUserUuid())) {
                String userUuid = rpcConnect.getUserUuid();
                if (targetIds.contains(userUuid)) {
                    if (cacheManage.getTerminalStatus(rpcConnect.getUserUuid()).equals(TerminalStatus.meeting.name())) {
                        continue;
                    }
                    params.addProperty(ProtocolElements.INVITE_PARTICIPANT_TARGET_ID_PARAM, userUuid);
                    this.notificationService.sendNotification(rpcConnect.getParticipantPrivateId(),
                            ProtocolElements.INVITE_PARTICIPANT_METHOD, params);
                }
            }
        }
    }

}
