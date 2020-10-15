package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.AppointParticipantMapper;
import io.openvidu.server.common.manage.AppointParticipantManage;
import io.openvidu.server.common.pojo.AppointParticipant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Collections.singletonList;

@Service
public class AppointParticipantManageImpl implements AppointParticipantManage {

    @Autowired
    private AppointParticipantMapper appointParticipantMapper;


    public List<AppointParticipant> listByRuid(String ruid) {
        return appointParticipantMapper.selectByRuids(singletonList(ruid));
    }

}
