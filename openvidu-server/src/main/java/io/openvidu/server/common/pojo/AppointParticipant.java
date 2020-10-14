package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class AppointParticipant {
    private Long id;

    private String ruid;

    private Long userId;

    private String uuid;

    private Integer status;

    private String project;

    private Date createTime;

    private Date updateTime;
}