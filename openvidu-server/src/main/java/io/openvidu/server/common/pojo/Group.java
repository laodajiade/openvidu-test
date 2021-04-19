package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;
import java.util.Objects;

@Data
public class Group {
    private Long id;

    private String groupName;

    private Long corpId;

    private String project;

    private Date createTime;

    private Date updateTime;

    private Integer numOfPeople;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return Objects.equals(id, group.id) &&
                Objects.equals(groupName, group.groupName) &&
                Objects.equals(corpId, group.corpId) &&
                Objects.equals(project, group.project) &&
                Objects.equals(createTime, group.createTime) &&
                Objects.equals(updateTime, group.updateTime) &&
                Objects.equals(numOfPeople, group.numOfPeople);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, groupName, corpId, project, createTime, updateTime, numOfPeople);
    }
}
