package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class ConferenceSearch {
    private String roomId;
    private int status;
    private String project;

    private int limit;
    private long offset;
    private Date from;
    private Date to;
}