package io.openvidu.server.common.pojo;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class FixedRoomManager implements Serializable {
    /**
     * 自增主键
     */
    private Long id;

    /**
     * 外键
     */
    private Long fixedId;

    private String roomId;

    /**
     * 企业id
     */
    private Long corpId;

    /**
     * 管理员
     */
    private Integer userId;

    /**
     * 管理员

     */
    private String uuid;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 删除状态
     */
    private Boolean deleted;

    private static final long serialVersionUID = 1L;
}