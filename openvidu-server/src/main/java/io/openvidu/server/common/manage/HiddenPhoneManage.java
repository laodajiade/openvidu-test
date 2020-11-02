package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.AllUserInfo;
import io.openvidu.server.common.pojo.UserGroupVo;

import java.util.List;

public interface HiddenPhoneManage {
    void hiddenPhone(List<UserGroupVo> userGroups);

    void hiddenPhone2(List<AllUserInfo> allUserInfos);
}
