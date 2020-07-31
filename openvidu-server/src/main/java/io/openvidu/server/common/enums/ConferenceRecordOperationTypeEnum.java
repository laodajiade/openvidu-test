package io.openvidu.server.common.enums;

public enum ConferenceRecordOperationTypeEnum {
    DOWNLOAD("下载"),
    PLAYBACK("回放"),
    DELETE("删除");
    private String desc;

    ConferenceRecordOperationTypeEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
