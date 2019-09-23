package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

@Data
public class Conference {
    private Long id;

    private String roomId;

    private String subject;

    private String desc;

    private Date startTime;

    private Date endTime;

    private Integer roomCapacity;

    private Integer status;

    private String password;

    private Integer inviteLimit;

    private Date createTime;

    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}