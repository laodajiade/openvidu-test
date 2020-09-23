package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class UserDept {
    private Long id;

    private Long userId;

    private Long deptId;

    private String project;

    private Date createTime;

    private Date updateTime;

}
