package io.openvidu.server.common.cache;

/**
 * @author chosongi
 * @date 2019/9/12 14:50
 */
public interface CacheManage {
    boolean accessTokenEverValid(String userId, String token);

    String getUserId(String uuid);
}
