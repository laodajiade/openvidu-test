package io.openvidu.server.common.pojo;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class HiddenUserGroup implements Serializable {
    private Long id;

    private Long corpId;

    /**
     * 1=全员隐藏  2=部分可见
     */
    private Integer hiddenType;

    private LocalDateTime gtmCreate;

    private static final long serialVersionUID = 1L;
}