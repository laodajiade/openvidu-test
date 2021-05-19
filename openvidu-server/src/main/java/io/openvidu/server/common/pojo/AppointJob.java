package io.openvidu.server.common.pojo;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class AppointJob implements Serializable {
    private Integer id;

    /**
     * 事件名称
     */
    private String scheduleName;

    private String ruid;

    /**
     * 启动时间
     */
    private LocalDateTime startTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 0=为执行  1=执行中 2=执行完毕  3=执行失败
     */
    private Integer status;

    /**
     * 执行时间
     */
    private LocalDateTime execTime;

    /**
     * 参数，json
     */
    private String params;

    private static final long serialVersionUID = 1L;
}