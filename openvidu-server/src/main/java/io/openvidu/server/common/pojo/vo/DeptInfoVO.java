package io.openvidu.server.common.pojo.vo;

import lombok.Data;

@Data
public class DeptInfoVO {
    /**
     * 父级部门的id，如果是查询根部门传0
     */
    private Long deptId;

    private Boolean isChooseAll;

    private Integer pageNum;
    private Integer pageSize;

    private Integer version;

    private String project;
}
