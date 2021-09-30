package io.openvidu.server.domain.vo;

import lombok.Data;

/**
 * @program:
 * @description:
 * @author: WuBing
 * @create: 2021-09-29 15:46
 **/
@Data
public class FixEDRoomScheduleVO {
    String ruid;

    String roomId;

    String subject;

    String startTime;

    int duration;

    String  desc;

    String creatorUsername;

    String creatorAccount;

    String endTime;

    int status;
}
