package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ConferencePartHistory {
    private Long id;

    private String ruid;

    private Long userId;

    private String uuid;

    private String username;

    private Integer userType;

    private String terminalType;

    private Integer status;

    private Date startTime;

    private Date endTime;

    private Integer duration;

    private String accessKey;

    private String project;

    private Date createTime;

    private Date updateTime;
}