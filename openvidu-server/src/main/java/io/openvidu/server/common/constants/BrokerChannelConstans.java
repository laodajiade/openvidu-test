package io.openvidu.server.common.constants;

public class BrokerChannelConstans {
    public static final String DEVICE_UPGRADE_CHANNEL = "channel:device:upgrade";
    public static final String USER_DELETE_CHANNEL = "channel:user:delete";

    /**
     * 客户端WS链接异常记录Key
     * ws:link:exception:{sessionId}:{privateId}
     * eg. ws:link:exception:80100201111:q7vs5c61tskvvesghg98cvpvcu
     */
    public static final String CLIENT_WS_EXCEPTION_KEY = "ws:link:exception:%s:%s";
}
