package io.openvidu.server.core;

public enum SessionPresetUseIDEnum {
    ONLY_MODERATOR("onlyModerator"),         // 仅主持人使用ID入会
    ALL_PARTICIPANTS("allParticipants");        // 所有参与者都可以使用ID入会

    private String message;
    SessionPresetUseIDEnum(String message) {
        this.message = message;
    }

    public String getMessage() { return this.message; }
}
