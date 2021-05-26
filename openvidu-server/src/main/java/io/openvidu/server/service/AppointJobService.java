package io.openvidu.server.service;

import io.openvidu.server.common.dao.AppointJobMapper;
import io.openvidu.server.common.pojo.AppointJob;
import io.openvidu.server.utils.LocalDateTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class AppointJobService {

    @Autowired
    private AppointJobMapper appointJobMapper;


    public List<AppointJob> selectNextJobs() {
        return appointJobMapper.selectNextJobs();
    }

    /**
     *
     * @param ruid
     * @param startTime 会议的开始时间
     */
    public void FiveMinuteBeforeTheBegin(String ruid, Date startTime) {
        appointJobMapper.cancelByRuid(ruid, "FiveMinuteBeforeTheBegin");

        //
        LocalDateTime eventTime = LocalDateTimeUtils.translateFromDate(startTime).minusMinutes(5);
        // 如果已经过了5分钟则不通知
        if (eventTime.isBefore(LocalDateTime.now())) {
            log.info("discard FiveMinuteBeforeTheBegin event ruid {}", ruid);
            return;
        }

        AppointJob job = new AppointJob();
        job.setScheduleName("FiveMinuteBeforeTheBegin");
        job.setRuid(ruid);
        job.setStartTime(eventTime);
        job.setRemark("会议开始前5分钟");
        job.setParams("{}");
        addJob(job);
    }

    /**
     *
     * @param ruid
     * @param startTime 会议的开始时间
     */
    public void OneMinuteBeforeTheBegin(String ruid, Date startTime) {
        appointJobMapper.cancelByRuid(ruid, "OneMinuteBeforeTheBegin");

        AppointJob job = new AppointJob();
        job.setScheduleName("OneMinuteBeforeTheBegin");
        job.setRuid(ruid);
        job.setStartTime(LocalDateTimeUtils.translateFromDate(DateUtils.addMinutes(startTime, -2)));
        job.setRemark("会议开始前2分钟");
        job.setParams("{}");
        addJob(job);
    }

    public void closeRoomSchedule(String ruid, Date colseTime) {
        AppointJob job = new AppointJob();
        job.setScheduleName("closeRoomSchedule");
        job.setRuid(ruid);
        job.setStartTime(LocalDateTimeUtils.translateFromDate(colseTime));
        job.setRemark("关闭会议1分钟倒计时");
        job.setParams("{}");
        addJob(job);
    }

    public void cancelCloseRoomSchedule(String ruid) {
        appointJobMapper.cancelByRuid(ruid, "closeRoomSchedule");
    }

    public void addJob(AppointJob job) {
        job.setCreateTime(LocalDateTime.now());
        job.setStatus(0);
        job.setExecTime(null);
        appointJobMapper.insert(job);
    }

    public synchronized boolean doExec(AppointJob appointJob) {
        return appointJobMapper.doExec(appointJob.getId()) > 0;
    }

    public boolean finishExec(AppointJob appointJob) {
        return appointJobMapper.finishExec(appointJob.getId()) > 0;
    }

    public boolean errorExec(AppointJob appointJob) {
        return appointJobMapper.errorExec(appointJob.getId()) > 0;
    }
}