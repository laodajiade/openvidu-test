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
        public boolean needToDisPatchSubtitle() {
            return false;
        }

        @Override
        public boolean ableToSendSubtitle() {
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
        public boolean needToDispatchTranslation() {
            return true;
        }
    };

    public boolean needToDisPatchSubtitle() {
        return true;
    }

    public boolean needToDispatchTranslation() {
        return false;
    }

    public boolean ableToSendSubtitle() {
        return true;
    }
}
