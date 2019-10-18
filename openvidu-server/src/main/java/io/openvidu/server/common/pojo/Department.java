package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Department {
    private Long id;

    private Long parentId;

    private String deptName;

    private Long corpId;

    private String project;

    private Date createTime;

    private Date updateTime;
}