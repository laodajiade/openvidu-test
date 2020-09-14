package io.openvidu.server.common.pojo;

import lombok.Data;

/**
 * @author even
 * @date 2020/9/10 17:12
 */
@Data
public class RootDept {

    private Long id;

    private String deptName;

    private Long parentId;

    private Long corpId;
}
