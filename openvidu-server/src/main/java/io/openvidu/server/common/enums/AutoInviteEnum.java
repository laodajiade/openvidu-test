package io.openvidu.server.common.enums;

public enum AutoInviteEnum {
    AUTO_INVITE(1, "自动呼叫"),
    DISABLE_AUTO_INVITE(0, "禁止自动呼叫");

    private Integer value;
    private String desc;

    AutoInviteEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
