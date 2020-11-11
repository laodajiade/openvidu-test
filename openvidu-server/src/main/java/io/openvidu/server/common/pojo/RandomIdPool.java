package io.openvidu.server.common.pojo;

import java.io.Serializable;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class RandomIdPool implements Serializable {
    private Integer id;

    private Long roomId;

    private static final long serialVersionUID = 1L;
}