package io.openvidu.server.common.pojo;

import lombok.Data;

@Data
public class ConferenceSearch {
    private String roomId;
    private int status;
    private String project;
}