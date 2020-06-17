package io.openvidu.server.common.enums;

/**
 * @author chosongi
 * @date 2020/6/17 10:09
 */
public enum SubtitleConfigEnum {
    /**
     * 关闭字幕功能（默认）
     */
    Off {

        @Override
        public boolean needToSendSubtitle() {
            return false;
        }
    },

    /**
     * 开启实时转写
     */
    ASR,

    /**
     * 开启实时翻译
     */
    Translate {

        @Override
        public boolean needToSendTranslation() {
            return true;
        }
    };

    public boolean needToSendSubtitle() {
        return true;
    }

    public boolean needToSendTranslation() {
        return false;
    }
}
