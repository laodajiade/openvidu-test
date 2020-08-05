package io.openvidu.server.common.enums;

/**
 * @author geedow
 * @date 2019/9/20 18:21
 */
public enum StreamType {
    /** 主码流*/
    MAJOR,

    /** 辅码流*/
    MINOR,

    /** 分享码流*/
    SHARING {
        @Override
        public boolean isSelfStream() {
            return false;
        }
    };

    public boolean isSelfStream() {
        return true;
    }
}
