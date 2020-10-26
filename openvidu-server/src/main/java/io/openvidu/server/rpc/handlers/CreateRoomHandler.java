package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionPreset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author geedow
 * @date 2019/11/5 16:18
 */
@Slf4j
@Service
public class CreateRoomHandler extends RpcAbstractHandler {

    @Autowired
    private AppointConferenceManage appointConferenceManage;


    @Transactional
    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String sessionId = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ID_PARAM);
        String password = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_PASSWORD_PARAM);
        String roomSubject = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SUBJECT_PARAM);
        String ruid = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_RUID_PARAM);
        String moderatorPassword = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_MODERATORPASSWORD_PARAM);
        ConferenceModeEnum conferenceMode = ConferenceModeEnum.valueOf(getStringParam(request,
                ProtocolElements.CREATE_ROOM_CONFERENCE_MODE_PARAM));
        if (StringUtils.isEmpty(sessionId)) {
            sessionId = generalRoomId();
        }
        //判断发起会议时是否超出企业人数上限
        Collection<Session> sessions = sessionManager.getSessions();
        if (Objects.nonNull(sessions)) {
            AtomicInteger limitCapacity = new AtomicInteger();
            sessions.forEach(e -> {
                if (rpcConnection.getProject().equals(e.getConference().getProject())) {
                    limitCapacity.addAndGet(e.getMajorPartEachConnect().size());
                }
            });
            //query sd_corporation info
            Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
            if (Objects.nonNull(corporation.getCapacity()) && limitCapacity.get() > corporation.getCapacity() - 1) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.ROOM_CAPACITY_CORP_LIMITED);
                return;
            }
        }
        if (isExistingRoom(sessionId, rpcConnection.getUserUuid())) {
            log.warn("conference:{} already exist.", sessionId);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
            return;
        }

        if (sessionManager.isNewSessionIdValid(sessionId)) {
            Conference conference = new Conference();
            // if create appointment conference
            if (!StringUtils.isEmpty(ruid)) {
                AppointConference appt = appointConferenceManage.getByRuid(ruid);
                if (appt == null) {
                    notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                            null, ErrorCodeEnum.APPOINTMENT_CONFERENCE_NOT_EXIST);
                    return;
                }
                if (appt.getStatus() == ConferenceStatus.FINISHED.getStatus()) {
                    notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                            null, ErrorCodeEnum.APPOINTMENT_CONFERENCE_HAS_FINISHED);
                    return;
                }
                appt.setStatus(ConferenceStatus.PROCESS.getStatus());
                appointConferenceManage.updateById(appt);

                conference.setRuid(ruid);
                roomSubject = appt.getConferenceSubject();
            } else {
                conference.setRuid(UUID.randomUUID().toString());
            }

            JsonObject respJson = new JsonObject();
            respJson.addProperty(ProtocolElements.CREATE_ROOM_ID_PARAM, sessionId);
            // save conference info
            conference.setRoomId(sessionId);
            conference.setConferenceSubject(roomSubject);
            conference.setConferenceMode(conferenceMode.getMode());
            conference.setUserId(rpcConnection.getUserId());
            conference.setPassword(StringUtils.isEmpty(password) ? null : password);
            conference.setStatus(1);
            conference.setStartTime(new Date());
            conference.setProject(rpcConnection.getProject());
            conference.setModeratorPassword(StringUtils.isEmpty(moderatorPassword) ? StringUtil.getRandomPassWord(6) : moderatorPassword);
            roomManage.createMeetingRoom(conference);

            // setPresetInfo.
            String micStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_MIC_STATUS_PARAM);
            String videoStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_VIDEO_STATUS_PARAM);
            String sharePowerInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_SHARE_POWER_PARAM);
            Integer roomCapacity = getIntOptionalParam(request, ProtocolElements.CREATE_ROOM_ROOM_CAPACITY_PARAM);
            Float roomDuration = getFloatOptionalParam(request, ProtocolElements.CREATE_ROOM_DURATION_PARAM);
            String useIdInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_USE_ID_PARAM);
            String allowPartOperMic = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOW_PART_OPER_MIC_PARAM);
            String allowPartOperShare = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_ALLOW_PART_OPER_SHARE_PARAM);
            String quietStatusInRoom = getStringOptionalParam(request, ProtocolElements.CREATE_ROOM_QUIET_STATUS_PARAM);

            SessionPreset preset = new SessionPreset(micStatusInRoom, videoStatusInRoom, sharePowerInRoom,
                    roomSubject, roomCapacity, roomDuration, useIdInRoom, allowPartOperMic, allowPartOperShare, quietStatusInRoom);
            sessionManager.setPresetInfo(sessionId, preset);

            // store this inactive session
            sessionManager.storeSessionNotActiveWhileRoomCreated(sessionId);
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
        } else {
            log.warn("conference:{} already exist.", sessionId);
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.CONFERENCE_ALREADY_EXIST);
        }
    }
}
