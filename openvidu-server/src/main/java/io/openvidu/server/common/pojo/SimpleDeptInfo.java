package io.openvidu.server.common.pojo;

import lombok.Data;

@Data
public class SimpleDeptInfo {

    private Long deptId;

    private String deptName;

    private Long parentId;

    private Boolean hasSubOrg;

    private Boolean hasMember;
    private Integer numOfEmp;

}
