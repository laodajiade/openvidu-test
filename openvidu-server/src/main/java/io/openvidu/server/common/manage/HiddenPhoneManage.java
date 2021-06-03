package io.openvidu.server.common.manage;

import java.util.List;

public interface HiddenPhoneManage {
    void hiddenPhone(List<?> list);

    /**
     * 查看用户是否被隐藏手机号
     * @param uuid
     * @return
     */
    boolean isHiddenUserPhone(String uuid);
}
