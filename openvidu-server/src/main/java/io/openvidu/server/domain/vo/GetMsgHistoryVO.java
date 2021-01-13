package io.openvidu.server.domain.vo;

import lombok.Data;

@Data
public class GetMsgHistoryVO {

    private long time;

    private String ruid;

    private int limit = 20;

    /**
     * 1按时间正序排列，2按时间降序排列
     */
    private int reverse = 2;
}
