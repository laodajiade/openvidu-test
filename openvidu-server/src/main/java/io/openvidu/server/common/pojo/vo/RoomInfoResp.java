package io.openvidu.server.common.pojo.vo;

import com.google.gson.annotations.JsonAdapter;
import io.openvidu.server.common.enums.RoomIdTypeEnums;
import io.openvidu.server.common.enums.RoomStateEnum;
import io.openvidu.server.utils.LocalDateTimeAdapter;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
public class RoomInfoResp implements Serializable {

    private String roomId;

    /**
     * 会议室名称
     */
    private String roomName;

    private RoomStateEnum state;

    /**
     * 容量
     */
    private Integer capacity;

    private String shortId;

    /**
     * 启用时间
     */
    @JsonAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime activationDate;

    /**
     * 到期时间
     */
    @JsonAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime expireDate;


    private RoomIdTypeEnums roomIdType;

    private ConferenceInfoResp conferenceInfo;

    private static final long serialVersionUID = 1L;
}