package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ConferenceRecord {
    private Long id;

    private String ruid;

    private String roomId;

    private Integer recordCount;

    private Integer totalDuration;

    private Integer status;

    private String recorderUuid;

    private String recorderName;

    private String project;

    private String accessKey;

    private Date requestStartTime;

    private Date createTime;

    private Date updateTime;

}
