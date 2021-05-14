package io.openvidu.server.common.pojo;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class FixedRoom implements Serializable {
    /**
     * 自增主键
     */
    private Long id;

    /**
     * 会议室名称
     */
    private String roomName;

    private String roomId;

    /**
     * 企业id
     */
    private Long corpId;

    /**
     * 短号
     */
    private Integer shortId;

    /**
     * 对应的充值卡id
     */
    private Long cardId;

    /**
     * 容量
     */
    private Integer roomCapacity;

    /**
     * 入会密码
     */
    private String password;

    /**
     * 主持密码
     */
    private String moderatorPassword;

    /**
     * 项目属性
     */
    private String project;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 启用时间
     */
    private LocalDateTime activationDate;

    /**
     * 到期时间
     */
    private LocalDateTime expireDate;

    /**
     * 允许呼入类型 0=不限制 
     */
    private Integer allowPart;

    /**
     * 状态 0 = 未激活  1= 正常 2 =  3 = 到期
     */
    private Integer status;

    /**
     * 允许录制  0/1
     */
    private Boolean allowRecord;

    /**
     * 删除状态
     */
    private Boolean deleted;

    private static final long serialVersionUID = 1L;
}