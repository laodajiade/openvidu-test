package io.openvidu.server.common.pojo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
public class DepartmentTree implements Serializable  {
    private Long orgId;
    private Long parentId;
    private String organizationName;
    private List<DepartmentTree> organizationList;
}