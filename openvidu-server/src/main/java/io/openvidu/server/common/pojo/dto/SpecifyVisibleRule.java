package io.openvidu.server.common.pojo.dto;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class SpecifyVisibleRule {
    /**
     * 0=全部隐藏  1=指定人员  2=不在限制名单中
     */
    private Integer type;

    private Set<Long> visibleUser = new HashSet<>();

    public SpecifyVisibleRule(Integer type) {
        this.type = type;
    }
}
