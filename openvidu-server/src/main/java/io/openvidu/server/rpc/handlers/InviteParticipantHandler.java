package io.openvidu.server.rpc.handlers;

import cn.jpush.api.push.model.notification.IosAlert;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.common.enums.TerminalTypeEnum;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.JpushMsgEnum;
import io.openvidu.server.core.Participant;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

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
            Set<Participant> majorParts = sessionManager.getSession(sessionId).getParticipants();
            if ((majorParts.size() + targetIds.size()) > preset.getRoomCapacity()) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.ROOM_CAPACITY_PERSONAL_LIMITED);
                return;
            }
        }

        //query sd_corporation info
        int joinNum = conferencePartHistoryMapper.countProcessPartHistory(rpcConnection.getProject());
        Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
        if (Objects.nonNull(corporation.getCapacity()) && targetIds.size() + joinNum > corporation.getCapacity()) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.ROOM_CAPACITY_CORP_LIMITED);
            return;
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
                cacheManage.saveInviteInfo(sessionId, invitee.getUuid());
                JsonObject customParams = params.deepCopy();
                customParams.addProperty(ProtocolElements.INVITE_PARTICIPANT_TARGET_ID_PARAM, invitee.getUuid());
                addInviteCompensation(invitee.getUuid(), customParams, expireTime);
            });
        }
        inviteOnline(targetIds, params);
        List<String> collect = sessionManager.getSession(sessionId).getParticipants().stream().map(Participant::getUuid).collect(Collectors.toList());
        JSONObject result = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        invitees.forEach(x -> {
            if (!collect.contains(x.getUuid())) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uuid", x.getUuid());
                jsonObject.put("userName", x.getUsername());
                jsonObject.put("accountType", x.getType());
                jsonArray.add(jsonObject);
            }
        });
        result.put("inviteList", jsonArray);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), result);
        Conference conference = sessionManager.getSession(sessionId).getConference();
        saveCallHistoryUsers(invitees, sessionId, conference.getRuid());
        //????????????
        Thread thread = new Thread(() -> sendJpushMessage(targetIds, moderatorName, conference.getConferenceSubject(), conference.getRuid()));
        thread.setName("invite-jpush-thread-" + sessionId);
        thread.start();

    }


    public void inviteOnline(List<String> targetIds, JsonObject params) {
        List<RpcConnection> rpcConnections = this.notificationService.getRpcConnectionByUuids(targetIds);

        for (RpcConnection rpcConnect : rpcConnections) {
            if (cacheManage.getTerminalStatus(rpcConnect.getUserUuid()).equals(TerminalStatus.meeting.name())) {
                continue;
            }
            params.addProperty(ProtocolElements.INVITE_PARTICIPANT_TARGET_ID_PARAM, rpcConnect.getUserUuid());
            this.notificationService.sendNotification(rpcConnect.getParticipantPrivateId(),
                    ProtocolElements.INVITE_PARTICIPANT_METHOD, params);
        }
    }

    private void sendJpushMessage(List<String> targetIds, String moderatorName, String subject, String ruid) {
        log.info("???????????? ????????????,ruid {},targetIds:{}", ruid, targetIds);
        targetIds.forEach(uuid -> {
            boolean send = false;
            Map userInfo = cacheManage.getUserInfoByUUID(uuid);
            if (Objects.nonNull(userInfo) && !userInfo.isEmpty()) {
                if (Objects.nonNull(userInfo.get("type")) && Objects.nonNull(userInfo.get("registrationId"))) {
                    //bug-fix:http://task.sudi.best/browse/BASE121-2631
                    List<RpcConnection> rpcConnections = this.notificationService.getRpcConnectionByUuids(uuid);
                    if (rpcConnections.isEmpty() || !Objects.equals(TerminalStatus.meeting.name(), userInfo.get("status"))) {
                        String type = userInfo.get("type").toString();

                        String registrationId = userInfo.get("registrationId").toString();
                        Date createDate = new Date();
                        String title = StringUtil.INVITE_CONT;
                        String alert = String.format(StringUtil.ON_MEETING_INVITE, moderatorName, subject);
                        Map<String, String> map = new HashMap<>(1);
                        map.put("message", jpushManage.getJpushMsgTemp(ruid, title, alert, createDate, JpushMsgEnum.MEETING_INVITE.getMessage()));
                        if (TerminalTypeEnum.A.name().equals(type)) {
                            jpushManage.sendToAndroid(title, alert, map, registrationId);
                            send = true;
                        } else if (TerminalTypeEnum.I.name().equals(type)) {
                            IosAlert iosAlert = IosAlert.newBuilder().setTitleAndBody(title, null, alert).build();
                            jpushManage.sendToIos(iosAlert, map, registrationId);
                            send = true;
                        }
                        jpushManage.saveJpushMsg(uuid, ruid, JpushMsgEnum.MEETING_INVITE.getMessage(), alert, createDate);
                    }
                }
            }
            if (!send) {
                log.info("???????????? ???????????? ????????? {}", uuid);
            }
        });
    }

}
