package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Conference {
    private Long id;

    private String roomId;

    private String conferenceSubject;

    private String conferenceDesc;

    private Date startTime;

    private Date endTime;

    private Integer roomCapacity;

    private Integer status;

    private String password;

    private Integer inviteLimit;

    private Date createTime;

    private Date updateTime;
}