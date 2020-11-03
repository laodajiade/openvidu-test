package io.openvidu.server.common.pojo.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class HiddenSpecifyVisibleDTO {
    /**
     * 0=全部隐藏  1=指定人员  2=不在限制名单中
     */
    private Integer type;


    /**
     * 0=未开启  1=部门以及部门下的人
     */
    private Integer deptVisible = Integer.MIN_VALUE;

    /**
     * 能看见的人，user_id数组
     */
    private Set<Long> visibleUser = new HashSet<>();
    private Set<Long> visibleDept = new HashSet<>();

    private Long userId;

    public HiddenSpecifyVisibleDTO(Integer type) {
        this.type = type;
    }

    public HiddenSpecifyVisibleDTO() {
    }
}
