package io.openvidu.server.core;

import org.springframework.util.StringUtils;

import java.util.Objects;

public class SessionPreset {

    private SessionPresetEnum micStatusInRoom;

    private SessionPresetEnum videoStatusInRoom;

    private SessionPresetEnum sharePowerInRoom;

    private String roomSubject;

    private int roomCapacity;

    private float roomDuration;       // 单位小时

    private SessionPresetUseIDEnum useIdTypeInRoom;

    private SessionPresetEnum allowPartOperMic;

    private SessionPresetEnum allowPartOperShare;

    private SessionPresetEnum allowPartOperSpeaker;

    private SessionPresetEnum quietStatusInRoom;

    private SessionPresetEnum pollingStatusInRoom = SessionPresetEnum.off;

    private Integer pollingIntervalTime;

    public void setMicStatusInRoom(SessionPresetEnum micStatusInRoom) { this.micStatusInRoom = micStatusInRoom; }

    public SessionPresetEnum getMicStatusInRoom() { return this.micStatusInRoom; }

    public void setVideoStatusInRoom(SessionPresetEnum micStatusInRoom) { this.videoStatusInRoom = micStatusInRoom; }

    public SessionPresetEnum getVideoStatusInRoom() { return this.micStatusInRoom; }

    public void setSharePowerInRoom(SessionPresetEnum sharePowerInRoom) { this.sharePowerInRoom = sharePowerInRoom; }

    public SessionPresetEnum getSharePowerInRoom() { return this.sharePowerInRoom; }

    public void setRoomSubject(String roomSubject) { this.roomSubject = roomSubject; }

    public String getRoomSubject() { return this.roomSubject; }

    public int getRoomCapacity() { return this.roomCapacity; }

    public void setRoomCapacity(int roomCapacity) { this.roomCapacity = roomCapacity; }

    public float getRoomDuration() { return this.roomDuration; }

    public void setRoomDuration(float roomDuration) { this.roomDuration = roomDuration; }

    public SessionPresetUseIDEnum getUseIdTypeInRoom() { return this.useIdTypeInRoom; }

    public void setUseIdTypeInRoom(SessionPresetUseIDEnum useIdType) { this.useIdTypeInRoom = useIdType; }

    public void setAllowPartOperMic(SessionPresetEnum allowPartOperMic) { this.allowPartOperMic = allowPartOperMic; }

    public SessionPresetEnum getAllowPartOperMic() { return this.allowPartOperMic; }

    public void setAllowPartOperShare(SessionPresetEnum allowPartOperShare) { this.allowPartOperShare = allowPartOperShare; }

    public SessionPresetEnum getAllowPartOperShare() { return this.allowPartOperShare; }

    public SessionPresetEnum getAllowPartOperSpeaker() {
        return allowPartOperSpeaker;
    }

    public void setAllowPartOperSpeaker(SessionPresetEnum allowPartOperSpeaker) {
        this.allowPartOperSpeaker = allowPartOperSpeaker;
    }

    public SessionPresetEnum getQuietStatusInRoom() {
        return quietStatusInRoom;
    }

    public void setQuietStatusInRoom(SessionPresetEnum quietStatusInRoom) {
        this.quietStatusInRoom = quietStatusInRoom;
    }

    public SessionPresetEnum getPollingStatusInRoom() {
        return pollingStatusInRoom;
    }

    public void setPollingStatusInRoom(SessionPresetEnum pollingStatusInRoom) {
        this.pollingStatusInRoom = pollingStatusInRoom;
    }

    public Integer getPollingIntervalTime() {
        return pollingIntervalTime;
    }

    public void setPollingIntervalTime(Integer pollingIntervalTime) {
        this.pollingIntervalTime = pollingIntervalTime;
    }

    public SessionPreset() {
        this.micStatusInRoom = SessionPresetEnum.off;
        this.videoStatusInRoom = SessionPresetEnum.on;
        this.sharePowerInRoom = SessionPresetEnum.off;
        this.roomSubject = "sudiRoom";
        this.roomCapacity = 500;
        this.roomDuration = -1f;
        this.useIdTypeInRoom = SessionPresetUseIDEnum.ALL_PARTICIPANTS;
        this.allowPartOperMic = SessionPresetEnum.off;
        this.allowPartOperShare = SessionPresetEnum.off;
        this.allowPartOperSpeaker = SessionPresetEnum.on;
        this.quietStatusInRoom = SessionPresetEnum.smart;
    }

    public SessionPreset(String micStatusInRoom,
                         String videoStatusInRoom,
                         String sharePowerInRoom,
                         String subject,
                         Integer roomCapacity,
                         Float roomDuration,
                         String useIdTypeInRoom,
                         String allowPartOperMic,
                         String allowPartOperShare,
                         String quietStatusInRoom) {
        this.micStatusInRoom = SessionPresetEnum.off;
        this.videoStatusInRoom = SessionPresetEnum.on;
        this.sharePowerInRoom = SessionPresetEnum.off;
        this.roomSubject = subject;
        this.roomCapacity = 500;
        this.roomDuration = -1f;
        this.useIdTypeInRoom = SessionPresetUseIDEnum.ALL_PARTICIPANTS;
        this.allowPartOperMic = SessionPresetEnum.off;
        this.allowPartOperShare = SessionPresetEnum.off;
        this.allowPartOperSpeaker = SessionPresetEnum.on;
        this.quietStatusInRoom = SessionPresetEnum.smart;

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

        // TODO. Fixme. Maybe have good way to do it.
        if (!StringUtils.isEmpty(useIdTypeInRoom)) {
            SessionPresetUseIDEnum[] useIds = SessionPresetUseIDEnum.values();
            for (SessionPresetUseIDEnum useId : useIds) {
                if (useId.getMessage().equalsIgnoreCase(useIdTypeInRoom)) {
                    this.useIdTypeInRoom = useId;
                    break;
                }
            }
        }

        if (!StringUtils.isEmpty(allowPartOperMic)) {
            this.allowPartOperMic = SessionPresetEnum.valueOf(allowPartOperMic);
        }

        if (!StringUtils.isEmpty(allowPartOperShare)) {
            this.allowPartOperShare = SessionPresetEnum.valueOf(allowPartOperShare);
        }

        if (!StringUtils.isEmpty(quietStatusInRoom)) {
            this.quietStatusInRoom = SessionPresetEnum.valueOf(quietStatusInRoom);
        }
    }
}
