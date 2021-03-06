package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class UserRole {
    private Long id;

    private Long userId;

    private Long roleId;

    private String project;

    private Date createTime;

    private Date updateTime;

}