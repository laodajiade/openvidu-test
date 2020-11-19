package io.openvidu.server.domain;

import lombok.Data;

import java.util.Date;

@Data
public class AppointConferenceDTO {

    /**
     * 会议创建者ID
     */
    private Long userId;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    private String project;


    private boolean onlyCreator = false;

    /**
     * 0=未开始 1=会议中  2=已结束
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
