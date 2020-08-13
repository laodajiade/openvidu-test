package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class StatisticsConferenceDaily {
    private Long id;

    private Long confCount;

    private Long totalDuration;

    private Long totalParticipants;

    private Long maxConcurrent;

    private Date statisticTime;

    private String accessKey;

    private String project;

    private Date createTime;

    private Date updateTime;
}
