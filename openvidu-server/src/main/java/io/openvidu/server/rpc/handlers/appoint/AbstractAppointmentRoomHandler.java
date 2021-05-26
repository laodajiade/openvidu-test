package io.openvidu.server.rpc.handlers.appoint;

import cn.jpush.api.push.model.notification.IosAlert;
import com.google.gson.JsonObject;
import com.sensegigit.cockcrow.CrowOnceHelper;
import com.sensegigit.cockcrow.enums.NotifyHandler;
import com.sensegigit.cockcrow.pojo.CrowOnceResponse;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.dao.CallHistoryMapper;
import io.openvidu.server.common.dao.FixedRoomManagerMapper;
import io.openvidu.server.common.dao.FixedRoomMapper;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.dao.JpushMessageMapper;
import io.openvidu.server.common.enums.*;
import io.openvidu.server.common.manage.ConferenceJobManage;
import io.openvidu.server.common.pojo.*;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.common.pojo.vo.CallHistoryVo;
import io.openvidu.server.core.JpushManage;
import io.openvidu.server.core.JpushMsgEnum;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.service.AppointJobService;
import io.openvidu.server.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractAppointmentRoomHandler<T> extends ExRpcAbstractHandler<T> {

    @Resource
    protected CrowOnceHelper crowOnceHelper;

    @Resource
    protected ConferenceJobManage conferenceJobManage;

    @Resource
    protected CacheManage cacheManage;

    @Resource
    protected JpushManage jpushManage;

    @Resource
    private CallHistoryMapper callHistoryMapper;


    @Autowired
    protected FixedRoomMapper fixedRoomMapper;

    @Autowired
    protected FixedRoomManagerMapper fixedRoomManagerMapper;

    @Autowired
    private AppointJobService appointJobService;

    /**
     * 创建定时任务
     */
    public void createTimer(AppointmentRoomVO vo, Set<String> uuidSet, JsonObject respJson) {

        // 定时任务
        String jobDesc = vo.getSubject() + "_" + vo.getRuid();
        Date startTime = new Date(vo.getStartTime());
        Date now = new Date();
        SafeSleep.sleepMilliSeconds(2000);
        List<ConferenceJob> list = new ArrayList<>();
        // 定时任务（会议开始时自动呼入）
        if (true) {
            Date beginDate = startTime;
            if ((startTime.getTime() - System.currentTimeMillis()) < 5000) {
                beginDate = new Date(System.currentTimeMillis() + 5000);
            }
            CrowOnceResponse crowOnceResponse = crowOnceHelper.addCrowOnce(CrowOnceInfoManager.createCrowOnceInfo(JobGroupEnum.XXL_JOB_EXECUTOR_CONFERENCE_NOTIFY.getId(),
                    NotifyHandler.conferenceBeginJobHandler, beginDate, jobDesc, respJson));
            if (Objects.nonNull(crowOnceResponse) && Objects.nonNull(crowOnceResponse.getCode()) && crowOnceResponse.getCode().intValue() == 200) {
                Long jobId = Long.valueOf(crowOnceResponse.getContent().toString());
                list.add(ConferenceJob.builder().ruid(vo.getRuid()).jobId(jobId).type(ConferenceJobTypeEnum.BEGIN.getType()).build());
            }
        }

        Date time = DateUtil.getEndDate(startTime, -15, Calendar.MINUTE);
        if (time.after(now)) {
            // 定时任务（会议开始前15分钟通知）
            CrowOnceResponse crowOnceResponse = crowOnceHelper.addCrowOnce(CrowOnceInfoManager.createCrowOnceInfo(JobGroupEnum.XXL_JOB_EXECUTOR_CONFERENCE_NOTIFY.getId(),
                    NotifyHandler.conferenceToBeginJobHandler, time, jobDesc, respJson));
            if (Objects.nonNull(crowOnceResponse) && Objects.nonNull(crowOnceResponse.getCode()) && crowOnceResponse.getCode().intValue() == 200) {
                Long jobId = Long.valueOf(crowOnceResponse.getContent().toString());
                list.add(ConferenceJob.builder().ruid(vo.getRuid()).jobId(jobId).type(ConferenceJobTypeEnum.TO_BEGIN.getType()).build());
            }
            // send appointment conference created notify
            sendAppointmentConferenceCreatedNotify(vo, uuidSet);
        } else if (startTime.after(now)) {
            // 会议开始距离现在不足15分钟
            // sendMessage
            cacheManage.publish(CacheKeyConstants.CONFERENCE_TO_BEGIN_NOTIFY, vo.getRuid());
            // sendNotify
            sendConferenceToBeginNotify(vo, uuidSet);
        }
        if (!list.isEmpty()) {
            // 保存ruid和jobId关系
            conferenceJobManage.batchInsert(list);
        }

        if (vo.getRoomIdType() == RoomIdTypeEnums.fixed) {
            appointJobService.FiveMinuteBeforeTheBegin(vo.getRuid(), new Date(vo.getStartTime()));
            appointJobService.OneMinuteBeforeTheBegin(vo.getRuid(), new Date(vo.getStartTime()));
        }
    }

    public void sendAppointmentConferenceCreatedNotify(AppointmentRoomVO vo, Set<String> uuidSet) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_STARTTIME_PARAM, vo.getStartTime());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_SUBJECT_PARAM, vo.getSubject());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_ENDTIME_PARAM, DateUtil.getEndDate(new Date(vo.getStartTime()), vo.getDuration(), Calendar.MINUTE).getTime());

        UserDeviceDeptInfo creator = userMapper.queryUserInfoByUserId(vo.getUserId());
        String userName = Objects.isNull(creator) ? "已注销"
                : StringUtils.isEmpty(creator.getUsername()) ? creator.getDeviceName() : creator.getUsername();
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_CREATOR_PARAM, userName);
        notificationService.getRpcConnections().forEach(rpcConnection1 -> {
            if (uuidSet.contains(rpcConnection1.getUserUuid()) && !(Objects.equals(rpcConnection1.getUserId(), vo.getUserId()) && vo.getAccessType() == AccessTypeEnum.terminal)) {
                notificationService.sendNotification(rpcConnection1.getParticipantPrivateId(), ProtocolElements.APPOINTMENT_CONFERENCE_CREATED_METHOD, jsonObject);
            }
        });
        //推送消息
        sendNotificationWithMeetingInvite(creator.getUuid(), uuidSet, userName, vo);
    }

    public void sendConferenceToBeginNotify(AppointmentRoomVO vo, Set<String> uuidSet) {
        JsonObject jsonObject = new JsonObject();
        UserDeviceDeptInfo creator = userMapper.queryUserInfoByUserId(vo.getUserId());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_CREATOR_PARAM, Objects.isNull(creator) ? "已注销"
                : StringUtils.isEmpty(creator.getUsername()) ? creator.getDeviceName() : creator.getUsername());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_SUBJECT_PARAM, vo.getSubject());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_STARTTIME_PARAM, vo.getStartTime());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_ENDTIME_PARAM, DateUtil.getEndDate(new Date(vo.getStartTime()), vo.getDuration(), Calendar.MINUTE).getTime());
        notificationService.getRpcConnections().forEach(rpcConnection1 -> {
            if (uuidSet.contains(rpcConnection1.getUserUuid())) {
                notificationService.sendNotification(rpcConnection1.getParticipantPrivateId(), ProtocolElements.CONFERENCE_TO_BEGIN_METHOD, jsonObject);
            }
        });
        //推送消息
        sendNotificationWithMeetingToBegin(uuidSet, vo);
    }

    protected List<AppointParticipant> constructBatchAppoints(String ruid, List<User> users) {
        List<AppointParticipant> appointParticipantList = new ArrayList<>(users.size());
        users.forEach(user -> {
            AppointParticipant appointParticipant = new AppointParticipant();
            appointParticipant.setRuid(ruid);
            appointParticipant.setUuid(user.getUuid());
            appointParticipant.setUserId(user.getId());
            appointParticipant.setStatus(ConferenceStatus.NOT_YET.getStatus());
            appointParticipant.setProject(user.getProject());
            appointParticipant.setCreateTime(new Date());

            appointParticipantList.add(appointParticipant);
        });

        return appointParticipantList;
    }

    protected void insertBatchCallHistory(String roomId, String ruid, List<User> users) {
        List<CallHistory> addList = new ArrayList<>();
        List<CallHistoryVo> callHistories = callHistoryMapper.getCallHistoryList(ruid);
        if (!CollectionUtils.isEmpty(callHistories)) {
            List<String> list = callHistories.stream().map(CallHistoryVo::getUuid).collect(Collectors.toList());
            users.forEach(invitee -> {
                if (!list.contains(invitee.getUuid())) {
                    CallHistory callHistory = new CallHistory();
                    callHistory.setRoomId(roomId);
                    callHistory.setUuid(invitee.getUuid());
                    callHistory.setUsername(invitee.getUsername());
                    callHistory.setRuid(ruid);
                    addList.add(callHistory);
                }
            });
        } else {
            users.forEach(invitee -> {
                CallHistory callHistory = new CallHistory();
                callHistory.setRoomId(roomId);
                callHistory.setUuid(invitee.getUuid());
                callHistory.setUsername(invitee.getUsername());
                callHistory.setRuid(ruid);
                addList.add(callHistory);
            });
        }
        if (!CollectionUtils.isEmpty(addList)) {
            callHistoryMapper.insertBatch(addList);
        }
    }

    @Async
    public void sendNotificationWithMeetingInvite(String moderatorUuid, Set<String> uuidSet, String userName, AppointmentRoomVO vo) {
        if (!CollectionUtils.isEmpty(uuidSet)) {
            uuidSet.forEach(uuid -> {
                if (uuid.equals(moderatorUuid)) {
                    return;
                }
                Map userInfo = cacheManage.getUserInfoByUUID(uuid);
                if (Objects.nonNull(userInfo) && !userInfo.isEmpty()) {
                    if (Objects.nonNull(userInfo.get("type")) && Objects.nonNull(userInfo.get("registrationId"))) {
                        String type = userInfo.get("type").toString();
                        String registrationId = userInfo.get("registrationId").toString();
                        Date createDate = new Date();
                        String title = StringUtil.INVITE_CONT;
                        String alert = String.format(StringUtil.MEETING_INVITE, userName, vo.getSubject(),
                                DateUtil.getDateFormat(new Date(vo.getStartTime()), DateUtil.DEFAULT_MONTH_DAY_HOUR_MIN),
                                DateUtil.getTimeOfDate(DateUtil.getEndDate(new Date(vo.getStartTime()), vo.getDuration(), Calendar.MINUTE).getTime()));
                        Map<String, String> map = new HashMap<>(1);
                        map.put("message", jpushManage.getJpushMsgTemp(vo.getRuid(), title, alert, createDate, JpushMsgEnum.MEETING_INVITE.getMessage()));
                        if (TerminalTypeEnum.A.name().equals(type)) {
                            jpushManage.sendToAndroid(title, alert, map, registrationId);
                        } else if (TerminalTypeEnum.I.name().equals(type)) {
                            IosAlert iosAlert = IosAlert.newBuilder().setTitleAndBody(title, null, alert).build();
                            jpushManage.sendToIos(iosAlert, map, registrationId);
                        }
                        jpushManage.saveJpushMsg(uuid, vo.getRuid(), JpushMsgEnum.MEETING_INVITE.getMessage(), alert, createDate);
                    }
                }
            });
        }
    }

    @Async
    public void sendNotificationWithMeetingToBegin(Set<String> uuidSet, AppointmentRoomVO vo) {
        if (!CollectionUtils.isEmpty(uuidSet)) {
            uuidSet.forEach(uuid -> {
                Map userInfo = cacheManage.getUserInfoByUUID(uuid);
                if (Objects.nonNull(userInfo) && !userInfo.isEmpty()) {
                    if (Objects.nonNull(userInfo.get("type")) && Objects.nonNull(userInfo.get("registrationId"))) {
                        String type = userInfo.get("type").toString();
                        String registrationId = userInfo.get("registrationId").toString();
                        log.info("send jpush message type:{} registrationId:{}", type, registrationId);
                        Date createDate = new Date();
                        String title = StringUtil.NOTIFY_CONT;
                        String alert = String.format(StringUtil.MEETING_NOTIFY, vo.getSubject(),
                                DateUtil.getDateFormat(new Date(vo.getStartTime()), DateUtil.DEFAULT_MONTH_DAY_HOUR_MIN),
                                DateUtil.getTimeOfDate(DateUtil.getEndDate(new Date(vo.getStartTime()), vo.getDuration(), Calendar.MINUTE).getTime()));
                        Map<String, String> map = new HashMap<>(1);
                        map.put("message", jpushManage.getJpushMsgTemp(vo.getRuid(), title, alert, createDate, JpushMsgEnum.MEETING_NOTIFY.getMessage()));

                        if (TerminalTypeEnum.A.name().equals(type)) {
                            jpushManage.sendToAndroid(title, alert, map, registrationId);
                        } else if (TerminalTypeEnum.I.name().equals(type)) {
                            IosAlert iosAlert = IosAlert.newBuilder().setTitleAndBody(title, null, alert).build();
                            jpushManage.sendToIos(iosAlert, map, registrationId);
                        }
                        jpushManage.saveJpushMsg(uuid, vo.getRuid(), JpushMsgEnum.MEETING_NOTIFY.getMessage(), alert, createDate);
                    }
                }
            });
        }
    }

    protected ErrorCodeEnum checkService(RpcConnection rpcConnection, AppointmentRoomVO params) {
        if (RoomIdTypeEnums.calculationRoomType(params.getRoomId()) == RoomIdTypeEnums.fixed) {
            return fixedVerification(rpcConnection, params);
        } else {
            return generalVerification(rpcConnection, params);
        }
    }

    protected ErrorCodeEnum fixedVerification(RpcConnection rpcConnection, AppointmentRoomVO params) {
        FixedRoom fixedRoom = fixedRoomMapper.selectByRoomId(params.getRoomId());
        if (fixedRoom == null || fixedRoom.getStatus() == 0) {
            return ErrorCodeEnum.CONFERENCE_NOT_EXIST;
        }
        if (fixedRoom.getStatus() == 2 || fixedRoom.getExpireDate().isBefore(LocalDateTime.now())) {
            return ErrorCodeEnum.FIXED_ROOM_EXPIRED;
        }

        FixedRoomManager fixedRoomManager = fixedRoomManagerMapper.selectByUserId(rpcConnection.getUserId(), fixedRoom.getRoomId());
        if (fixedRoomManager == null) {
            return ErrorCodeEnum.PERMISSION_LIMITED;
        }

        // 检验容量
        params.setRoomCapacity(fixedRoom.getRoomCapacity());
        if (fixedRoom.getRoomCapacity() < params.getParticipants().size()) {
            return ErrorCodeEnum.ROOM_CAPACITY_LIMITED;
        }

        return ErrorCodeEnum.SUCCESS;
    }

    protected ErrorCodeEnum generalVerification(RpcConnection rpcConnection, AppointmentRoomVO params) {
        Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
        //判断通话时长是否不足
        if (Objects.nonNull(corporation) && corporation.getRemainderDuration() <= 0) {
            return ErrorCodeEnum.REMAINDER_DURATION_USE_UP;
        }
        // 检验容量
        params.setRoomCapacity(corporation.getCapacity());
        if (corporation.getCapacity() < params.getParticipants().size()) {
            return ErrorCodeEnum.ROOM_CAPACITY_LIMITED;
        }

        // 校验有效期
        if (LocalDateTimeUtils.toEpochMilli(corporation.getExpireDate()) < params.getEndTime()) {
            return ErrorCodeEnum.APPOINTMENT_TIME_AFTER_SERVICE_EXPIRED;
        }

        if (!corporationMapper.isConcurrentServiceDuration(corporation)) {
            return ErrorCodeEnum.SERVICE_NOT_ACTIVATION_OR_EXPIRED;
        }
        return ErrorCodeEnum.SUCCESS;
    }
}
