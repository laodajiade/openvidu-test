package io.openvidu.server.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class PartHisResp {

    private Long id;

    /**
     * RUID（会议唯一ID）
     */
    private String ruid;

    /**
     * 会议室名称
     */
    private String roomId;

    /**
     * 主题
     */
    private String subject;

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 会议时长（分钟）
     */
    private Integer duration;

}
