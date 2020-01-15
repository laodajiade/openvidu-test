package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class UserDevice {
    private Long id;

    private Long userId;

    private String serialNumber;

    private String project;

    private Date createTime;

    private Date updateTime;
}