package io.openvidu.server.common.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @author 
 * 预约会议表
 */
@Data
public class AppointConference implements Serializable {
    /**
     * 自增主键
     */
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
    private String conferenceSubject;

    /**
     * 会议描述
     */
    private String conferenceDesc;

    /**
     * 会议创建者ID
     */
    private Long userId;

    /**
     * 主持人uuid
     */
    private String moderatorUuid;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 会议时长（分钟）
     */
    private Integer duration;

    /**
     * 容量
     */
    private Integer roomCapacity;

    /**
     * 入会密码
     */
    private String password;

    /**
     * 0：设置自动呼叫；1：禁止自动呼叫（仅当预约会议时，该字段存在设置意义）
     */
    private Integer autoInvite;

    /**
     * 项目属性
     */
    private String project;

    /**
     * 类型（普通预约会议：N，周期性预约会议：P）
     */
    private String type;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private Integer conferenceMode;

    /**
     * 0=未开始 1=会议中  2=已结束
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}