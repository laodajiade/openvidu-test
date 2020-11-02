package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.UserGroupVo;

import java.util.List;

public interface HiddenPhoneManage {
    void hiddenPhone(List<UserGroupVo> userGroups);
}
