package io.openvidu.server.domain.vo;

import lombok.Data;

@Data
public class GetConferenceScheduleVO extends PageVO {

    private String status;
    private String date;
    private String startDate;
    private String endDate;

    private Boolean onlyCreator = false;

}
