package io.openvidu.server.core;

import com.github.pagehelper.Page;
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


    public PageResult() {
    }

    public PageResult(Collection<T> list, Page page) {
        this.list = list;
        this.total = page.getTotal();
        this.pages = page.getPages();
        this.pageNum = page.getPageNum();
        this.pageSize = page.getPageSize();
    }
}
