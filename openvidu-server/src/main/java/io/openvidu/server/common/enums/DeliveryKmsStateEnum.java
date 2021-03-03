package io.openvidu.server.common.enums;

public enum DeliveryKmsStateEnum {

    /**
     * 初始状态
     */
    CONNECTING,
    /**
     * Pipeline已连接
     */
    CONNECTED,
    /**
     * 可用阶段
     */
    READY,
    /**
     * 不可用状态
     */
    FAILED;
}
