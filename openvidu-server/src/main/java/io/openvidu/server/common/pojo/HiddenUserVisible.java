package io.openvidu.server.common.pojo;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class HiddenUserVisible implements Serializable {
    private Long id;

    private Long hiddenUserGroupId;

    private Long userId;

    private String uuid;

    private Long corpId;

    private LocalDateTime gtmCreate;

    private static final long serialVersionUID = 1L;
}