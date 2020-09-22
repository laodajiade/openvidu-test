package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class User {
    private Long id;

    private String uuid;

    private String username;

    private String phone;

    private String email;

    private String password;

    private String project;

    private String title;

    private Date createTime;

    private Date updateTime;

    private String icon;
}
