package io.openvidu.server.common.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("sd_department")
public class Department {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String deptName;

    private Long corpId;

    private String project;

    private Date createTime;

    private Date updateTime;

    private Boolean hasSubOrg;

    private Boolean hasMember;

    private Integer numOfEmp;

}
