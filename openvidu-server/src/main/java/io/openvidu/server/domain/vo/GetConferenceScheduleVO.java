package io.openvidu.server.domain.vo;

import lombok.Data;

@Data
public class GetConferenceScheduleVO extends PageVO {

    private String status;
    private String date;
    private Boolean onlyCreator;

    private Boolean onlyCreator = false;

}
