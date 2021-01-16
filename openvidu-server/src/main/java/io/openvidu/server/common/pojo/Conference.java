package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Conference {
    private Long id;

    private String ruid;

    private String roomId;

    private String conferenceSubject;

    private String conferenceDesc;

    private Integer conferenceMode;

    private Long userId;

    private String moderatorUuid;

    private Date startTime;

    private Date endTime;

    private Integer roomCapacity;

    private Integer status;

    private String password;

    private Integer inviteLimit;

    private String project;

    private Date createTime;

    private Date updateTime;

    private String moderatorPassword;

    private Integer concurrentNumber;

    private String roomIdType;

    private String shortUrl;
}
