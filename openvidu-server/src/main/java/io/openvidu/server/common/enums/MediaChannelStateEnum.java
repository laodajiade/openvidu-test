package io.openvidu.server.common.enums;

public enum MediaChannelStateEnum {

    INITIAL {
        public boolean isInitStage() {
            return true;
        }
    },
    PREPARE {
        public boolean isInitStage() {
            return true;
        }
    },
    READY {
        @Override
        public boolean isAvailable() {
            return true;
        }
    },
    FLOWING {
        @Override
        public boolean isAvailable() {
            return true;
        }
    },
    FAILED,
    CLOSE,
    ;


    boolean initStage = false;

    boolean available = false;

    public boolean isInitStage() {
        return initStage;
    }

    public boolean isAvailable() {
        return available;
    }
}
