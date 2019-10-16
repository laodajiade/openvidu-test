package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Device {
    private Long id;

    private String serialNumber;

    private String deviceId;

    private String deviceName;

    private String deviceType;

    private String deviceModel;

    private String ability;

    private String version;

    private String manufacturer;

    private Byte accessType;

    private String project;

    private Date createTime;

    private Date updateTime;

}