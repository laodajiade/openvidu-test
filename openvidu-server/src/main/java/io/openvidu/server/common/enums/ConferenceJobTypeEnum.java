package io.openvidu.server.common.enums;

public enum ConferenceJobTypeEnum {
    TO_BEGIN("toBegin"),
    BEGIN("begin"),
    END("end");

    private String type;

    ConferenceJobTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
