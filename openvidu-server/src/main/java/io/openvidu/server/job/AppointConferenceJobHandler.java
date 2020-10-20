package io.openvidu.server.job;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sensegigit.cockcrow.CrowOnceHelper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.enums.AccessTypeEnum;
import io.openvidu.server.common.enums.AutoInviteEnum;
import io.openvidu.server.common.enums.ConferenceStatus;
import io.openvidu.server.common.enums.TerminalStatus;
import io.openvidu.server.common.manage.AppointConferenceManage;
import io.openvidu.server.common.manage.AppointParticipantManage;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.AppointParticipant;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.core.Session;
import io.openvidu.server.core.SessionManager;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcNotificationService;
import io.openvidu.server.rpc.handlers.appoint.CreateAppointmentRoomHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private UserManage userManage;

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


            // 是否自动呼叫、房间是否被使用中
            if (appointConference.getAutoInvite().intValue() == AutoInviteEnum.AUTO_INVITE.getValue().intValue() && !isRoomInUse(appointConference.getRoomId())) {
                // change the conference status
                Conference conference = constructConf(appointConference);
                conferenceMapper.insertSelective(conference);

                sessionManager.storeSessionNotActiveWhileAppointCreate(conference.getRoomId(), conference);

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

    @Scheduled(cron = "0 0/1 * * * ?")
    public void fixEndAppointment() {
        List<AppointConference> list = appointConferenceMapper.getMaybeEndAppointment();
        if (list.isEmpty()) {
            return;
        }
        Set<String> ruids = sessionManager.getSessions().stream().map(Session::getRuid).collect(Collectors.toSet());

        list.removeIf(appt -> ruids.contains(appt.getRuid()));

        if (list.size() == 0) {
            return;
        }

        for (AppointConference appointConference : list) {
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
        User moderator = userManage.getUserByUserId(conference.getUserId());


        JsonObject params = new JsonObject();
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_EXPIRETIME_PARAM, String.valueOf(System.currentTimeMillis() + 60000));
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_ID_PARAM, conference.getRoomId());
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_SOURCE_ID_PARAM, String.valueOf(moderator.getUuid()));
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_USERNAME_PARAM, Objects.isNull(moderator.getUsername()) ? "" : moderator.getUsername());
        params.addProperty(ProtocolElements.INVITE_PARTICIPANT_AUTO_INVITE_PARAM, conference.getAutoInvite());
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
        return conference;
    }

}
