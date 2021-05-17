package io.openvidu.server.common.pojo.vo;

import lombok.Data;

/**
 * @author Administrator
 */
@Data
public class UserDeptInfo {
    //d.id,d.dept_name deptName,d.parent_id parentId,d.corp_id corpId , su.id AS userId , su.username,su.phone,su.uuid,su.email
    private Long id;
    private String deptName;
    private Long parentId;
    private Long corpId;
    private Long userId;
    private String uuid;
    private String userName;
    private String phone;
    private String email;
    private String department;
    /**
     * 是否是常用联系人
     */
    private boolean isFrequentContact;
}
