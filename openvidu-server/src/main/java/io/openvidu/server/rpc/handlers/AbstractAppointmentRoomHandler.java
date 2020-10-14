package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import com.sensegigit.cockcrow.CrowOnceHelper;
import com.sensegigit.cockcrow.enums.NotifyHandler;
import com.sensegigit.cockcrow.pojo.CrowOnceResponse;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.constants.CacheKeyConstants;
import io.openvidu.server.common.enums.ConferenceJobTypeEnum;
import io.openvidu.server.common.enums.JobGroupEnum;
import io.openvidu.server.common.manage.ConferenceJobManage;
import io.openvidu.server.common.pojo.ConferenceJob;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.utils.CrowOnceInfoManager;
import io.openvidu.server.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
public abstract class AbstractAppointmentRoomHandler<T> extends ExRpcAbstractHandler<T> {

    @Resource
    protected CrowOnceHelper crowOnceHelper;

    @Resource
    protected ConferenceJobManage conferenceJobManage;

    /**
     * 创建定时任务
     */
    public void createTimer(AppointmentRoomVO vo, Set<String> uuidSet, JsonObject respJson) {

        // 定时任务
        String jobDesc = vo.getSubject() + "_" + vo.getRuid();
        Date startTime = new Date(vo.getStartTime());
        Date now = new Date();

        List<ConferenceJob> list = new ArrayList<>();
        // 定时任务（会议开始时自动呼入）
        if (startTime.after(now)) {
            CrowOnceResponse crowOnceResponse = crowOnceHelper.addCrowOnce(CrowOnceInfoManager.createCrowOnceInfo(JobGroupEnum.XXL_JOB_EXECUTOR_CONFERENCE_NOTIFY.getId(),
                    NotifyHandler.conferenceBeginJobHandler, startTime, jobDesc, respJson));
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
        // 保存ruid和jobId关系
        conferenceJobManage.batchInsert(list);
    }

    public void sendAppointmentConferenceCreatedNotify(AppointmentRoomVO vo, Set<String> uuidSet) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_STARTTIME_PARAM, vo.getStartTime());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_SUBJECT_PARAM, vo.getSubject());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_ENDTIME_PARAM, DateUtil.getEndDate(new Date(vo.getStartTime()), vo.getDuration(), Calendar.MINUTE).getTime());
        User creator = userManage.getUserByUserId(vo.getUserId());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_CREATOR_PARAM, Objects.nonNull(creator) ? creator.getUsername() : "");
        notificationService.getRpcConnections().forEach(rpcConnection1 -> {
            if (!Objects.equals(rpcConnection1.getUserId(), vo.getUserId()) && uuidSet.contains(rpcConnection1.getUserUuid())) {
                notificationService.sendNotification(rpcConnection1.getParticipantPrivateId(), ProtocolElements.APPOINTMENT_CONFERENCE_CREATED_METHOD, jsonObject);
            }
        });
    }

    public void sendConferenceToBeginNotify(AppointmentRoomVO vo, Set<String> uuidSet) {
        JsonObject jsonObject = new JsonObject();
        User creator = userManage.getUserByUserId(vo.getUserId());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_CREATOR_PARAM, Objects.nonNull(creator) ? creator.getUsername() : "");
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_SUBJECT_PARAM, vo.getSubject());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_STARTTIME_PARAM, vo.getStartTime());
        jsonObject.addProperty(ProtocolElements.CREATEAPPOINTMENTROOM_ENDTIME_PARAM, DateUtil.getEndDate(new Date(vo.getStartTime()), vo.getDuration(), Calendar.MINUTE).getTime());
        notificationService.getRpcConnections().forEach(rpcConnection1 -> {
            if (uuidSet.contains(rpcConnection1.getUserUuid())) {
                notificationService.sendNotification(rpcConnection1.getParticipantPrivateId(), ProtocolElements.CONFERENCE_TO_BEGIN_METHOD, jsonObject);
            }
        });
    }

}
