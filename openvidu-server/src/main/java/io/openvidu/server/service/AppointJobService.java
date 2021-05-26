package io.openvidu.server.service;

import io.openvidu.server.common.dao.AppointJobMapper;
import io.openvidu.server.common.pojo.AppointJob;
import io.openvidu.server.common.pojo.AppointJobExample;
import io.openvidu.server.utils.LocalDateTimeUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class AppointJobService {

    @Autowired
    private AppointJobMapper appointJobMapper;


    public List<AppointJob> selectNextJobs() {
        return appointJobMapper.selectNextJobs();
    }

    public void FiveMinuteBeforeTheBegin(String ruid, Date endTime) {
        appointJobMapper.cancelByRuid(ruid, "FiveMinuteBeforeTheBegin");

        AppointJob job = new AppointJob();
        job.setScheduleName("FiveMinuteBeforeTheBegin");
        job.setRuid(ruid);
        job.setStartTime(LocalDateTimeUtils.translateFromDate(DateUtils.addMinutes(endTime, -5)));
        job.setRemark("会议开始前5分钟");
        job.setParams("{}");
        addJob(job);
    }

    public void OneMinuteBeforeTheBegin(String ruid, Date endTime) {
        appointJobMapper.cancelByRuid(ruid, "OneMinuteBeforeTheBegin");

        AppointJob job = new AppointJob();
        job.setScheduleName("OneMinuteBeforeTheBegin");
        job.setRuid(ruid);
        job.setStartTime(LocalDateTimeUtils.translateFromDate(DateUtils.addMinutes(endTime, -2)));
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