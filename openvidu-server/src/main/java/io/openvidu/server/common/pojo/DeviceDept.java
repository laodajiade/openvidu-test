package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class DeviceDept {
    private Long id;

    private String serialNumber;

    private Long deptId;

    private Long corpId;

    private String project;

    private Date createTime;

    private Date updateTime;

    private String deviceName;

    private Long userId;

    private String uuid;

}