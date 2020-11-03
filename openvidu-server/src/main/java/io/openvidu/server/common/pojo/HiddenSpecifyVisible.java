package io.openvidu.server.common.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * @author 
 * 
 */
@Data
public class HiddenSpecifyVisible implements Serializable {
    private Long id;

    /**
     * 被限制的对象  uuid数组
     */
    private String limitUser;

    /**
     * 能看见的人，user_id数组
     */
    private String visibleUser;

    /**
     * 0=全部隐藏  1=指定人员
     */
    private Integer type;

    /**
     * 0=未开启  1=部门以及部门下的人
     */
    private Integer deptVisible;

    private Date gmtCreate;

    private Date gmtUpdate;

    private Long corpId;

    private static final long serialVersionUID = 1L;
}