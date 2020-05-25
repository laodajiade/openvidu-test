package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.ConferenceRecordInfo;
import io.openvidu.server.common.pojo.User;

public interface ConferenceRecordLogManage {
    void insertOperationLog(ConferenceRecordInfo conferenceRecordInfo, String type, User user);
}
