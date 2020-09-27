package io.openvidu.server.core;

import lombok.Data;

import java.util.Collection;

@Data
public class PageResult<T> {
    private Collection<T> list;

    private Integer pages;
    /**
     * 总记录数
     */
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
}
