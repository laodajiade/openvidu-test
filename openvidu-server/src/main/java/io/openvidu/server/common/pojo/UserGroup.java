package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class UserGroup {
    private Long id;

    private Long userId;

    private Long groupId;

    private String project;

    private Date createTime;

    private Date updateTime;

    private String groupName;

    private List<User>  userList;

}