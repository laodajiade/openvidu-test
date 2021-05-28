package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.AllUserInfo;
import io.openvidu.server.common.pojo.UserGroupVo;
import io.openvidu.server.common.pojo.vo.OftenContactsVo;

import java.util.List;

public interface HiddenPhoneManage {
    void hiddenPhone(List<UserGroupVo> userGroups);

    void hiddenPhone2(List<AllUserInfo> allUserInfos);

    void hiddenContactsPhone(List<OftenContactsVo> oftenContactsVos);

    /**
     * 查看用户是否被隐藏手机号
     * @param uuid
     * @return
     */
    boolean isHiddenUserPhone(String uuid);
}
