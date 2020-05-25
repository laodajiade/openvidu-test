package io.openvidu.server.common.enums;

public enum ConferenceRecordStatusEnum {
    WAIT(0, "等待录制"),
    PROCESS(1, "录制中"),
    FINISH(2, "录制完成");

    private Integer status;
    private String desc;

    ConferenceRecordStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

}
