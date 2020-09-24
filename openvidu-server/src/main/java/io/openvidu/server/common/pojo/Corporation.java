package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Corporation {
    private Long id;

    private String corpName;

    private String project;

    private Date createTime;

    private Date updateTime;

    private Integer capacity;

}
