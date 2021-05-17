package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @author Administrator
 */
@Data
public class OftenContacts {
    /**
     * 主键id
     */
    private Long id;
    /**
     * 常用联系人id
     */
    private Long contactsUserId;
    /**
     * 常用联系人uuid
     */
    private String contactsUuid;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 修改时间
     */
    private Date updateTime;

}
