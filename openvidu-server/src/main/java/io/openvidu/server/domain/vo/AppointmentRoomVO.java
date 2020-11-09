package io.openvidu.server.domain.vo;

import io.openvidu.server.common.enums.ConferenceModeEnum;
import io.openvidu.server.common.enums.RoomIdTypeEnums;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRoomVO {

    private String ruid;
    private String roomId;
    private String password;
    private String moderatorPassword;
    private String subject;
    private String desc;
    private String moderatorRoomId;
    private ConferenceModeEnum conferenceMode;
    private Boolean autoCall;
    private Integer roomCapacity;
    private Long startTime;
    private Long endTime;
    private Integer duration;
    private String creator;

    private List<String> participants;


    private Long userId;

    private RoomIdTypeEnums roomIdType;
}
