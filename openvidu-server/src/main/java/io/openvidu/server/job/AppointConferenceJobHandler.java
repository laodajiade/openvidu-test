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
import io.openvidu.server.common.dao.FixedRoomMapper;
import io.openvidu.server.common.dao.UserMapper;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.manage.AppointParticipantManage;
import io.openvidu.server.common.manage.RoomManage;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.core.*;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.rpc.handlers.appoint.CreateAppointmentRoomHandler;
import io.openvidu.server.service.AppointJobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
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

    @Resource
    private RoomManage roomManage;

    @Autowired
    private FixedRoomMapper fixedRoomMapper;

    @Autowired
    private AppointJobService appointJobService;

    @Scheduled(cron = "0/5 * * * * ?")
    public void appointmentJob() {
        List<AppointJob> appointJobs = appointJobService.selectNextJobs();
        if (appointJobs.isEmpty()) {
            return;
        }

        for (AppointJob appointJob : appointJobs) {
            try {
                if (!appointJobService.doExec(appointJob)) {
                    log.info("job id:{} can`t get lock", appointJob.getId());
                    continue;
                }
                String scheduleName = appointJob.getScheduleName();
                switch (scheduleName) {
                    case "FiveMinuteBeforeTheBegin":
                        fiveMinuteBeforeTheBegin(appointJob);
                        break;
                    case "OneMinuteBeforeTheBegin":
                        oneMinuteBeforeTheBegin(appointJob);
                        break;
                    case "closeRoomSchedule":
                        closeRoomSchedule(appointJob);
                        break;

                }
                appointJobService.finishExec(appointJob);
            } catch (Exception e) {
                log.error("appointmentJob error {}", appointJob, e);
                appointJobService.errorExec(appointJob);
            }
        }
    }

    private void closeRoomSchedule(AppointJob appointJob) {
        Conference conference = conferenceMapper.selectByRuid(appointJob.getRuid());
        if (conference == null || conference.getStatus() != 1) {
            return;
        }

        Session session = sessionManager.getSession(conference.getRoomId());
        if (session != null) {
            session.close(EndReason.sessionClosedByServer);
        }
    }

    private void oneMinuteBeforeTheBegin(AppointJob job) {
        nextConferenceToBeginNotify(job.getRuid(), 1);
    }

    /**
     * 向冲突的主持人推送下个会议开始的推送
     */
    private void fiveMinuteBeforeTheBegin(AppointJob job) {
        nextConferenceToBeginNotify(job.getRuid(), 5);
    }

    private void nextConferenceToBeginNotify(String ruid, int countdown) {
        AppointConference appointConference = appointConferenceManage.getByRuid(ruid);
        if (appointConference == null || appointConference.getStatus() != 0) {
            return;
        }

        Session session = sessionManager.getSession(appointConference.getRoomId());
        if (session == null) {
            return;
        }

        List<Participant> moderatorAndThorPart = session.getModeratorAndThorPart();
        if (!moderatorAndThorPart.isEmpty()) {
            JsonObject params = new JsonObject();
            params.addProperty("countdown", countdown);
            params.addProperty("startTime", appointConference.getStartTime().getTime());
            params.addProperty("moderatorName", appointConference.getModeratorName());
            params.addProperty("conferenceSubject", appointConference.getConferenceSubject());
            notificationService.sendBatchNotificationConcurrent(new HashSet<>(moderatorAndThorPart), "nextConferenceToBeginNotify", params);
        }

        // 创建一个1分钟倒计时关闭会议
        // 这里使用62而不是60是因为多给端上2秒的反应时间
        if (countdown == 1) {
            appointJobService.closeRoomSchedule(session.getRuid(), DateUtils.addSeconds(new Date(), 62));
        }
    }


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
            crowOnceHelper.delCrowOnce(jobId);
            return ReturnT.FAIL;
        }
        // 会议提前开始，不在提醒
        if (appointConference.getStatus() != ConferenceStatus.NOT_YET.getStatus()) {
            crowOnceHelper.delCrowOnce(jobId);
            return ReturnT.SUCCESS;
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
                    .duration(appointConference.getDuration()).subject(appointConference.getConferenceSubject()).ruid(ruid).build();
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

            // 是否自动呼叫、房间是否被使用中
            if (!isRoomInUse(appointConference.getRoomId())) {
                SessionPreset preset = new SessionPreset(SessionPresetEnum.on.name(), SessionPresetEnum.on.name(), null,
                        appointConference.getConferenceSubject(), appointConference.getRoomCapacity(), appointConference.getDuration().floatValue(), null, null, null, null);
                sessionManager.setPresetInfo(appointConference.getRoomId(), preset);
                if (RoomIdTypeEnums.calculationRoomType(appointConference.getRoomId()) == RoomIdTypeEnums.fixed) {
                    FixedRoom fixedRoom = fixedRoomMapper.selectByRoomId(appointConference.getRoomId());
                    preset.setAllowRecord(fixedRoom.getAllowRecord() ? SessionPresetEnum.on : SessionPresetEnum.off);
                    preset.setRoomCapacity(fixedRoom.getRoomCapacity());
                }

                // change the conference status
                Conference conference = constructConf(appointConference);
                conferenceMapper.insertSelective(conference);
                Session session = sessionManager.storeSessionNotActiveWhileAppointCreate(conference.getRoomId(), conference);
                session.setEndTime(appointConference.getEndTime().getTime());

                appointConferenceMapper.changeStatusByRuid(ConferenceStatus.PROCESS.getStatus(), appointConference.getRuid());
            } else {
                log.info("conferenceBeginJobHandler in use:{}", JSON.toJSONString(appointConference));
                //todo 修改定时任务在下一分钟
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
        log.info("fix end conference ruid = " + conference.getRuid());

        if (System.currentTimeMillis() - conference.getStartTime().getTime() <= 60000) {
            log.info("Conference survival time is less than 1 minute, ruid = {}", conference.getRuid());
            return;
        }

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
                    cacheManage.saveInviteInfo(conference.getRoomId(), rpcConnection.getUserUuid());
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
        conference.setModeratorPassword(ac.getModeratorPassword());
        conference.setRoomIdType(RoomIdTypeEnums.random.name());
        conference.setShortUrl(roomManage.createShortUrl());
        conference.setModeratorName(ac.getModeratorName());
        return conference;
    }

}
