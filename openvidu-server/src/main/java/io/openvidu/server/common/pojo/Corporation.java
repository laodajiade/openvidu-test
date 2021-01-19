package io.openvidu.server.common.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class Corporation {
    private Long id;

    private String corpName;

    private String project;

    private Date createTime;

    private Date updateTime;

    private Integer capacity;

    private Integer recordingCapacity;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime activationDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime expireDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime recordingActivationDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime recordingExpireDate;

    private Integer remainderDuration;
}
