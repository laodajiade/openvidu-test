package io.openvidu.server.common.constants;

public class BrokerChannelConstans {
    public static final String DEVICE_UPGRADE_CHANNEL = "channel:device:upgrade";
    public static final String USER_DELETE_CHANNEL = "channel:user:delete";
    public static final String CORP_SERVICE_EXPIRED_CHANNEL = "channel:corp:expired";
    public static final String CORP_INFO_MODIFIED_CHANNEL = "channel:corp:info:modified";
    public static final String TO_OPENVIDU_CHANNEL = "channel:to:openvidu";

    public static final String SMS_DELIVERY_CHANNEL = "channel:sms:delivery";

    /**
     * 客户端WS链接异常记录Key
     * ws:link:exception:{sessionId}:{privateId}
     * eg. ws:link:exception:80100201111:q7vs5c61tskvvesghg98cvpvcu
     */
    public static final String CLIENT_WS_EXCEPTION_KEY = "ws:link:exception:%s:%s";
    public static final String DEVICE_LOG_UPLOAD_CHANNEL = "channel:device:log:upload";
    public static final String DEVICE_NAME_UPDATE_CHANNEL = "channel:devicename:update";
    public static final String TOPIC_ROOM_RECORDER_ERROR = "recording-error";
}
