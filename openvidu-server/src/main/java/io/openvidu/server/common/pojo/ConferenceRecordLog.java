package io.openvidu.server.common.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ConferenceRecordLog {
    private Long id;

    private String type;

    private String operatorUuid;

    private String operatorUsername;

    private Long recordInfoId;

    private String ruid;

    private String recordName;

    private String recordDisplayName;

    private Long recordSize;

    private String thumbnailUrl;

    private String recordUrl;

    private Date startTime;

    private Date endTime;

    private Integer duration;

    private Date createTime;

}
