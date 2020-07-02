package io.openvidu.server.common.pojo;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class CorpMcuConfig {
    private Long id;

    private String resolution;

    private Integer fps;

    private Integer minsendkbps;

    private Integer maxsendkbps;

    private Integer gop;

    private String project;

    private Date createTime;

    private Date updateTime;
}