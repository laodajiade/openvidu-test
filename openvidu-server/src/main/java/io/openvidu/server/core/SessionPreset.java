package io.openvidu.server.core;

import org.springframework.util.StringUtils;

public class SessionPreset {

    private SessionPresetEnum micStatusInRoom;

    private SessionPresetEnum sharePowerInRoom;

    private SessionPresetEnum useIdInRoom;

    private String roomSubject;

    public void setMicStatusInRoom(SessionPresetEnum micStatusInRoom) { this.micStatusInRoom = micStatusInRoom; }

    public SessionPresetEnum getMicStatusInRoom() { return this.micStatusInRoom; }

    public void setSharePowerInRoom(SessionPresetEnum sharePowerInRoom) { this.sharePowerInRoom = sharePowerInRoom; }

    public SessionPresetEnum getSharePowerInRoom() { return this.sharePowerInRoom; }

    public void setUseIdInRoom(SessionPresetEnum useIdInRoom) { this.useIdInRoom = useIdInRoom; }

    public SessionPresetEnum getUseIdInRoom() { return this.useIdInRoom; }

    public void setRoomSubject(String roomSubject) { this.roomSubject = roomSubject; }

    public String getRoomSubject() { return this.roomSubject; }

    public SessionPreset() {
        this.micStatusInRoom = SessionPresetEnum.disable;
        this.sharePowerInRoom = SessionPresetEnum.disable;
        this.useIdInRoom = SessionPresetEnum.enable;
    }

    public SessionPreset(String micStatusInRoom, String sharePowerInRoom, String useIdInRoom, String subject) {
        this.micStatusInRoom = SessionPresetEnum.disable;
        this.sharePowerInRoom = SessionPresetEnum.disable;
        this.useIdInRoom = SessionPresetEnum.enable;
        this.roomSubject = subject;

        if (!StringUtils.isEmpty(micStatusInRoom)) {
            this.micStatusInRoom = SessionPresetEnum.valueOf(micStatusInRoom);
        }

        if (!StringUtils.isEmpty(sharePowerInRoom)) {
            this.sharePowerInRoom = SessionPresetEnum.valueOf(sharePowerInRoom);
        }

        if (!StringUtils.isEmpty(useIdInRoom)) {
            this.useIdInRoom = SessionPresetEnum.valueOf(useIdInRoom);
        }
    }
}
