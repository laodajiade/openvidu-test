package io.openvidu.server.common.enums;

public enum JobGroupEnum {
    XXL_JOB_EXECUTOR_CONFERENCE_NOTIFY(1L, "xxl-job-executor-conference-notify", "会议提醒执行器"),
    XXL_JOB_EXECUTOR_SAMPLE(2L, "xxl-job-executor-sample", "示例执行器");

    public Long id;
    public String appName;
    public String title;

    JobGroupEnum(Long id, String appName, String title) {
        this.appName = appName;
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
