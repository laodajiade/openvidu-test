package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Group {
    private Long id;

    private String groupName;

    private Long corpId;

    private String project;

    private Date createTime;

    private Date updateTime;
}