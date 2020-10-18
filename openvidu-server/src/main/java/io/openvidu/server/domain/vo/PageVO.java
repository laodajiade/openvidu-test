package io.openvidu.server.domain.vo;

import lombok.Data;

import java.util.Objects;

@Data
public class PageVO {

    private Integer pageNum;
    private Integer pageSize;

    private Boolean isChooseAll = false;

    public Integer getPageSize() {
        return Objects.equals(true, isChooseAll) ? -1 : pageSize;
    }
}
