package io.openvidu.server.core;

import org.springframework.util.StringUtils;

import java.util.Objects;

public class SessionPreset {

    private SessionPresetEnum micStatusInRoom;

    private SessionPresetEnum videoStatusInRoom;

    private SessionPresetEnum sharePowerInRoom;

    private String roomSubject;

    private int roomCapacity;

    private int roomDuration;

    public void setMicStatusInRoom(SessionPresetEnum micStatusInRoom) { this.micStatusInRoom = micStatusInRoom; }

    public SessionPresetEnum getMicStatusInRoom() { return this.micStatusInRoom; }

    public void setVideoStatusInRoom(SessionPresetEnum micStatusInRoom) { this.videoStatusInRoom = micStatusInRoom; }

    public SessionPresetEnum getVideoStatusInRoom() { return this.micStatusInRoom; }

    public void setSharePowerInRoom(SessionPresetEnum sharePowerInRoom) { this.sharePowerInRoom = sharePowerInRoom; }

    public SessionPresetEnum getSharePowerInRoom() { return this.sharePowerInRoom; }

    public void setRoomSubject(String roomSubject) { this.roomSubject = roomSubject; }

    public String getRoomSubject() { return this.roomSubject; }

    public SessionPreset() {
        this.micStatusInRoom = SessionPresetEnum.off;
        this.videoStatusInRoom = SessionPresetEnum.on;
        this.sharePowerInRoom = SessionPresetEnum.off;
        this.roomSubject = "sudiRoom";
        this.roomCapacity = 3;
        this.roomDuration = 600;
    }

    public SessionPreset(String micStatusInRoom,
                         String videoStatusInRoom,
                         String sharePowerInRoom,
                         String subject,
                         Integer roomCapacity,
                         Integer roomDuration) {
        this.micStatusInRoom = SessionPresetEnum.off;
        this.videoStatusInRoom = SessionPresetEnum.on;
        this.sharePowerInRoom = SessionPresetEnum.off;
        this.roomSubject = "sudiRoom";
        this.roomCapacity = 3;
        this.roomDuration = 600;

        if (!StringUtils.isEmpty(micStatusInRoom)) {
            this.micStatusInRoom = SessionPresetEnum.valueOf(micStatusInRoom);
        }

        if (!StringUtils.isEmpty(videoStatusInRoom)) {
            this.videoStatusInRoom = SessionPresetEnum.valueOf(videoStatusInRoom);
        }

        if (!StringUtils.isEmpty(sharePowerInRoom)) {
            this.sharePowerInRoom = SessionPresetEnum.valueOf(sharePowerInRoom);
        }

        if (!StringUtils.isEmpty(subject)) {
            this.roomSubject = subject;
        }

        if (!Objects.isNull(roomCapacity)) {
            this.roomCapacity = roomCapacity;
        }

        if (!Objects.isNull(roomDuration)) {
            this.roomDuration = roomDuration;
        }
    }
}
