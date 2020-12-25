package io.openvidu.server.job;

import com.alibaba.fastjson.JSON;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sensegigit.cockcrow.CrowOnceHelper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.java.client.OpenViduRole;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.UserMapper;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.manage.AppointParticipantManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.AppointParticipant;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.core.*;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.rpc.handlers.appoint.CreateAppointmentRoomHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AppointConferenceJobHandler {

    @Resource
    private CacheManage cacheManage;

    @Resource
    private RpcNotificationService notificationService;

    @Resource
    private CreateAppointmentRoomHandler createAppointmentRoomHandler;

    @Resource
    private UserMapper userMapper;

    @Resource
    private SessionManager sessionManager;

    @Autowired
    private AppointConferenceManage appointConferenceManage;

    @Autowired
    private AppointConferenceMapper appointConferenceMapper;

    @Autowired
    private AppointParticipantManage appointParticipantManage;

    @Resource
    private CrowOnceHelper crowOnceHelper;

    @Resource
    private ConferenceMapper conferenceMapper;

    /**
     * 预约会议通知
     *
     * @param param
     * @return
     */
    @XxlJob("conferenceToBeginJobHandler")
    public ReturnT<String> conferenceToBeginJobHandler(String param) {
        log.info("conferenceToBeginJobHandler begin..." + param);
        JsonObject jsonParam = new JsonParser().parse(param).getAsJsonObject();
        Long jobId = jsonParam.get(ProtocolElements.XXL_JOB_ID).getAsLong();
        JsonObject businessParam = jsonParam.get(ProtocolElements.XXL_JOB_PARAM).getAsJsonObject();
        String ruid = businessParam.get(ProtocolElements.CREATE_ROOM_RUID_PARAM).getAsString();

        // 会议不存在直接返回
        AppointConference appointConference = appointConferenceManage.getByRuid(ruid);
        if (Objects.isNull(appointConference)) {
            log.error("conferenceToBeginJobHandler conference not exist, ruid:{}", ruid);
            return ReturnT.FAIL;
        }

        // sendMessage
        cacheManage.publish(CacheKeyConstants.CONFERENCE_TO_BEGIN_NOTIFY, ruid);
        log.info("conferenceToBeginJobHandler sendSMS end...");


        // sendNotify
        List<AppointParticipant> appointParts = appointParticipantManage.listByRuid(ruid);

        if (Objects.nonNull(appointParts) && !appointParts.isEmpty()) {
            Set<String> uuidSet = appointParts.stream().map(AppointParticipant::getUuid).collect(Collectors.toSet());

            AppointmentRoomVO vo = AppointmentRoomVO.builder()
                    .userId(appointConference.getUserId()).startTime(appointConference.getStartTime().getTime())
                    .duration(appointConference.getDuration()).subject(appointConference.getConferenceSubject()).build();
            createAppointmentRoomHandler.sendConferenceToBeginNotify(vo, uuidSet);
            log.info("conferenceToBeginJobHandler notify end... uuidSet ={}", uuidSet);
        }
        // 删除定时任务
        crowOnceHelper.delCrowOnce(jobId);
        return ReturnT.SUCCESS;
    }

    /**
     * 预约会议开始自动呼入
     *
     * @param param
     * @return
     */
    @XxlJob("conferenceBeginJobHandler")
    public ReturnT<String> conferenceBeginJobHandler(String param) {
        try {
            log.info("conferenceBeginJobHandler begin..." + param);
            JsonObject jsonParam = new JsonParser().parse(param).getAsJsonObject();
            Long jobId = jsonParam.get(ProtocolElements.XXL_JOB_ID).getAsLong();
            JsonObject businessParam = jsonParam.get(ProtocolElements.XXL_JOB_PARAM).getAsJsonObject();
            String ruid = businessParam.get(ProtocolElements.CREATE_ROOM_RUID_PARAM).getAsString();
            // 获取会议信息
            AppointConference appointConference = appointConferenceManage.getByRuid(ruid);
            if (Objects.isNull(appointConference)) {
                log.error("conferenceBeginJobHandler conference not exist, ruid:{}", ruid);
                return ReturnT.FAIL;
            }

            if (appointConference.getStatus() != 0) {//会议已提前结束
                crowOnceHelper.delCrowOnce(jobId);
                return ReturnT.SUCCESS;
            }

            if (appointConference.getAutoInvite().intValue() != AutoInviteEnum.AUTO_INVITE.getValue().intValue()) {
                // 删除定时任务
                crowOnceHelper.delCrowOnce(jobId);
                return ReturnT.SUCCESS;
            }

            // 是否自动呼叫、房间是否被使用中
            if (!isRoomInUse(appointConference.getRoomId())) {
                SessionPreset preset = new SessionPreset(SessionPresetEnum.on.name(), SessionPresetEnum.on.name(), null,
                        appointConference.getConferenceSubject(), appointConference.getRoomCapacity(), appointConference.getDuration().floatValue(), null, null, null, null);
                sessionManager.setPresetInfo(appointConference.getRoomId(), preset);

                // change the conference status
                Conference conference = constructConf(appointConference);
                conferenceMapper.insertSelective(conference);
                Session session = sessionManager.storeSessionNotActiveWhileAppointCreate(conference.getRoomId(), conference);
                session.setEndTime(appointConference.getEndTime().getTime());

                appointConferenceMapper.changeStatusByRuid(ConferenceStatus.PROCESS.getStatus(), appointConference.getRuid());
            } else {
                log.info("conferenceBeginJobHandler non invite:{}", JSON.toJSONString(appointConference));
            }

            if (isSameRoom(appointConference.getRoomId(), ruid) && appointConference.getAutoInvite().equals(AutoInviteEnum.AUTO_INVITE.getValue())) {
                // sendNotify
                List<AppointParticipant> appointParts = appointParticipantManage.listByRuid(ruid);

                if (Objects.isNull(appointParts) || appointParts.isEmpty()) {
                    log.error("conferenceBeginJobHandler appointParts is empty, ruid:{}", ruid);
                    return ReturnT.FAIL;
                }
                // 邀请通知
                Set<String> uuidSet = appointParts.stream().map(AppointParticipant::getUuid).collect(Collectors.toSet());
                log.info("conferenceBeginJobHandler notify begin...uuidSet={}", uuidSet);
                inviteParticipant(appointConference, uuidSet);
            }


            // 删除定时任务
            crowOnceHelper.delCrowOnce(jobId);
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.info("conferenceBeginJobHandler error ", e);
            throw e;
        }

    }

    /**
     * 1、预约会议到了结束时间，如果会议没人则关闭会议室，否则推送延迟结束广告
     * 2、修复因异常重启等原因导致会议没有正常结束而导致状态不正确
     */
    @XxlJob("conferenceEndJobHandler")
    public ReturnT<String> conferenceEndJobHandler(String param) {
        conferenceEndJobHandler0(param);
        fixFinishConferenceStatus();
        return ReturnT.SUCCESS;
    }

    /**
     * 1、预约会议到了结束时间，如果会议没人则关闭会议室，否则推送延迟结束广告
     * 2、修复因异常重启等原因导致会议没有正常结束而导致状态不正确
     */
    private ReturnT<String> conferenceEndJobHandler0(String param) {
        List<AppointConference> list = appointConferenceMapper.getMaybeEndAppointment();
        if (list.isEmpty()) {
            return ReturnT.SUCCESS;
        }
        Collection<Session> sessions = sessionManager.getSessions();

        for (Session session : sessions) {
            Optional<AppointConference> first = list.stream().filter(appt -> session.getRuid().equals(appt.getRuid())).findFirst();
            if (!first.isPresent()) {
                continue;
            }

            list.removeIf(appt -> appt.getRuid().equals(session.getRuid()));

            Set<Participant> participants = session.getParticipants();
            if (participants.isEmpty()) {
                // finish conference
                log.info("conferenceEndJobHandler close session roomId = {}, ruid = {}", first.get().getRoomId(), first.get().getRuid());
                sessionManager.closeSession(first.get().getRoomId(), EndReason.sessionClosedByServer);
            } else {
                // notify
                if (session.isDelay()) {
                    continue;
                }

                JsonObject params = new JsonObject();
                params.addProperty("roomId", session.getSessionId());
                params.addProperty("ruid", session.getRuid());

                Optional<Participant> moderator = participants.stream().filter(participant -> participant.getStreamType() == StreamType.MAJOR && participant.getRole() == OpenViduRole.MODERATOR)
                        .findAny();
                moderator.ifPresent(participant -> notificationService.sendNotification(participant.getParticipantPrivateId(), ProtocolElements.ROOM_AUTO_DELAY_METHOD, params));
                log.info("conferenceEndJobHandler session delay roomId = {}, ruid = {}", session.getSessionId(), session.getRuid());
                session.setDelay(true);
            }
        }


        Set<String> ruids = sessionManager.getSessions().stream().map(Session::getRuid).collect(Collectors.toSet());

        list.removeIf(appt -> ruids.contains(appt.getRuid()));

        if (list.size() == 0) {
            return ReturnT.SUCCESS;
        }

        for (AppointConference appointConference : list) {
            finishConference(appointConference);
            log.info("conferenceEndJobHandler finish session roomId = {}, ruid = {}", appointConference.getRoomId(), appointConference.getRuid());
        }

        return ReturnT.SUCCESS;
    }

    private void finishConference(AppointConference appointConference) {
        log.info(" fix end appointment ruid= " + appointConference.getRuid());
        Conference conference = conferenceMapper.selectByRuid(appointConference.getRuid());
        if (conference == null) {
            conference = constructConf(appointConference);
            conference.setStartTime(appointConference.getEndTime());
            conference.setEndTime(appointConference.getEndTime());
            conference.setStatus(2);
            conferenceMapper.insertSelective(conference);
        } else {
            conference.setEndTime(new Date());
            conference.setStatus(2);
            conferenceMapper.updateByPrimaryKey(conference);
        }

        appointConference.setStatus(2);
        appointConferenceMapper.updateByPrimaryKey(appointConference);
    }


    private void fixFinishConferenceStatus() {
        List<Conference> list = conferenceMapper.getNotFinishConference();
        list.removeIf(conference -> conference.getRuid().startsWith("appt-"));//去掉预约会议
        if (list.isEmpty()) {
            return;
        }

        for (Conference conference : list) {
            if (!cacheManage.checkRoomLease(conference.getRoomId(), conference.getRuid())) {
                finishConference(conference);
            }
        }
    }

    private void finishConference(Conference conference) {
        log.info(" fix end conference ruid= " + conference.getRuid());
        conference.setEndTime(new Date());
        conference.setStatus(2);
        conferenceMapper.updateByPrimaryKey(conference);
    }

    /**
     * 向与会人发出会议开始呼叫
     *
     * @param conference 会议信息
     * @param uuidSet    与会人
     */
    public void inviteParticipant(AppointConference conference, Set<String> uuidSet) {
        if (uuidSet.isEmpty()) {
            return;
        }
        // 获取主持人信息
        UserDeviceDeptInfo moderator = userMapper.queryUserInfoByUserId(conference.getUserId());


        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_EXPIRETIME_PARAM, String.valueOf(System.currentTimeMillis() + 60000));
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_ID_PARAM, conference.getRoomId());
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM, String.valueOf(moderator.getUuid()));
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERNAME_PARAM, StringUtils.isEmpty(moderator.getUsername()) ? moderator.getDeviceName() : moderator.getUsername());
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_AUTO_INVITE_PARAM, conference.getAutoInvite());
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SUBJECT_PARAM, conference.getConferenceSubject());
        params.addProperty("ruid", conference.getRuid());
        if (!StringUtils.isEmpty(conference.getPassword())) {
            params.addProperty(ProtocolElements.INVITE_PARTICIPANT_PASSWORD_PARAM, conference.getPassword());
        }
        // 邀请通知
        notificationService.getRpcConnections()
                .stream()
                .filter(rpcConnection -> uuidSet.contains(rpcConnection.getUserUuid())
                        && Objects.equals(rpcConnection.getAccessType(), AccessTypeEnum.terminal)
                        && TerminalStatus.online.name().equals(cacheManage.getTerminalStatus(rpcConnection.getUserUuid())))
                .forEach(rpcConnection -> {
                    log.info("conferenceBeginJobHandler inviteParticipant uuid={}", rpcConnection.getUserUuid());
                    params.addProperty(ProtocolElements.INVITE_PARTICIPANT_TARGET_ID_PARAM, rpcConnection.getUserId());
                    notificationService.sendNotification(rpcConnection.getParticipantPrivateId(), ProtocolElements.INVITE_PARTICIPANT_METHOD, params);
                });
    }

    /**
     * 判断房间是否被占用
     *
     * @param roomId
     * @return
     */
    public boolean isRoomInUse(String roomId) {
        boolean isRoomInUse = !Objects.isNull(sessionManager.getSession(roomId)) || !Objects.isNull(sessionManager.getSessionNotActive(roomId));
        if (isRoomInUse) {
            log.info("conferenceBeginJobHandler roomId={} is in use", roomId);
        }
        return isRoomInUse;
    }

    /**
     * 判断房间是否被占用的同时判断是不是同一个预约会议
     *
     * @param roomId
     * @return
     */
    public boolean isSameRoom(String roomId, String ruid) {
        Session session = sessionManager.getSession(roomId);
        if (session != null && Objects.equals(session.getRuid(), ruid)) {
            log.info("roomId={}, ruid={} is used and same", roomId, ruid);
            return true;
        }
        Session sessionNotActive = sessionManager.getSessionNotActive(roomId);
        if (sessionNotActive != null && Objects.equals(sessionNotActive.getConference().getRuid(), ruid)) {
            log.info("roomId={}, ruid={} is used and same", roomId, ruid);
            return true;
        }
        log.info("roomId={}, ruid={} is used and not same", roomId, ruid);
        return false;
    }

    public Conference constructConf(AppointConference ac) {
        Conference conference = new Conference();

        // save conference info
        conference.setRoomId(ac.getRoomId());

        conference.setRuid(ac.getRuid());
        conference.setConferenceSubject(ac.getConferenceSubject());
        conference.setConferenceMode(ac.getConferenceMode());
        conference.setUserId(ac.getUserId());
        conference.setPassword(ac.getPassword());
        conference.setStatus(ConferenceStatus.PROCESS.getStatus());
        conference.setStartTime(ac.getStartTime());

        conference.setRoomCapacity(ac.getRoomCapacity());
        conference.setProject(ac.getProject());
        conference.setConferenceDesc(ac.getConferenceDesc());
        conference.setModeratorUuid(ac.getModeratorUuid());
        conference.setProject(ac.getProject());
        conference.setModeratorUuid(ac.getModeratorUuid());
        conference.setRoomIdType(RoomIdTypeEnums.random.name());
        return conference;
    }

}
