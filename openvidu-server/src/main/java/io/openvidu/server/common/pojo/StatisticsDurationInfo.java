package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.Date;

/**
 * @author even
 * @date 2020/11/9 17:50
 */
@Data
public class StatisticsDurationInfo {

    private String roomId;

    private String subject;

    private Long createTime;

    private Integer duration;
}
