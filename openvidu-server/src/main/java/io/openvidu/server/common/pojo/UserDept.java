package io.openvidu.server.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sd_user_dept")
public class UserDept {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long deptId;

    private String project;

    private Date createTime;

    private Date updateTime;

}
