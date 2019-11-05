package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Role {
    private Long id;

    private String roleName;

    private String roleDesc;

    private String privilege;

    private String project;

    private Date createTime;

    private Date updateTime;

}