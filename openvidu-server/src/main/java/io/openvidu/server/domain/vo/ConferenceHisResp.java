package io.openvidu.server.domain.vo;

import lombok.Data;

@Data
public class ConferenceHisResp {

    String uuid;

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
     * 会议描述
     */
    private String desc;

    /**
     * 主持人uuid
     */
    private String moderatorUuid;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 会议时长（分钟）
     */
    private Integer duration;

    /**
     * 容量
     */
    private Integer roomCapacity;

    /**
     * 会议发起人昵称
     */
    private String creatorUsername;

    /**
     * 会议发起人的account
     */
    private String creatorAccount;

    private Long creatorUserId;

    private String creatorUserIcon;

    private Integer status;

}
