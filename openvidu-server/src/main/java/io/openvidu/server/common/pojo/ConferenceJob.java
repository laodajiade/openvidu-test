package io.openvidu.server.common.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ConferenceJob {
    private Long id;

    private String ruid;

    private Long jobId;

    private String type;

    private Date createTime;

    private Date updateTime;

}