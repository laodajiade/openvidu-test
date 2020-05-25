package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.ConferenceRecordLogMapper;
import io.openvidu.server.common.manage.ConferenceRecordLogManage;
import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.common.pojo.ConferenceRecordLog;
import io.openvidu.server.common.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class ConferenceRecordLogManageImpl implements ConferenceRecordLogManage {
    @Resource
    private ConferenceRecordLogMapper conferenceRecordLogMapper;

    @Override
    public void insertOperationLog(ConferenceRecordInfo conferenceRecordInfo, String type, User operator) {
        // 保存操作日志
        conferenceRecordLogMapper.insertSelective(ConferenceRecordLog.builder()
                .type(type)
                .operatorUuid(operator.getUuid())
                .operatorUsername(operator.getUsername())
                .recordInfoId(conferenceRecordInfo.getId())
                .ruid(conferenceRecordInfo.getRuid())
                .recordName(conferenceRecordInfo.getRecordName())
                .recordDisplayName(conferenceRecordInfo.getRecordDisplayName())
                .recordSize(conferenceRecordInfo.getRecordSize())
                .thumbnailUrl(conferenceRecordInfo.getThumbnailUrl())
                .recordUrl(conferenceRecordInfo.getRecordUrl())
                .startTime(conferenceRecordInfo.getStartTime())
                .endTime(conferenceRecordInfo.getEndTime())
                .duration(conferenceRecordInfo.getDuration())
                .build());

    }

}
