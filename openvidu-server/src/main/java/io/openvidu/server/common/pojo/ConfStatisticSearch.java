package io.openvidu.server.common.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
public class ConfStatisticSearch {
    private Date startTime;
    private Date endTime;
    private String accessKey;
    private Integer status;
    private String project;
}
