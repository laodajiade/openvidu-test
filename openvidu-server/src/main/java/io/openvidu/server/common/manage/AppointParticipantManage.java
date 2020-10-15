package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.AppointParticipant;

import java.util.List;

public interface AppointParticipantManage {


    List<AppointParticipant> listByRuid(String ruid);
}
