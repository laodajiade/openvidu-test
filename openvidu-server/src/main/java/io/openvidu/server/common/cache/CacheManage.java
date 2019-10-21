package io.openvidu.server.common.cache;

import io.openvidu.server.common.enums.UserOnlineStatusEnum;

import java.util.Map;

/**
 * @author chosongi
 * @date 2019/9/12 14:50
 */
public interface CacheManage {

    Map getUserInfoByUUID(String userId);

    String getUserAuthorization(String userId);

    void updateUserOnlineStatus(String userId, UserOnlineStatusEnum onlineStatusEnum);
}
