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
 * 
 */
@Data
public class FixedRoomResp implements Serializable {
    /**
     * 自增主键
     */
    private Long id;

    /**
     * 会议室名称
     */
    private String roomName;

    private String roomId;

    /**
     * 短号
     */
    private Integer shortId;

    /**
     * 容量
     */
    private Integer capacity;

    /**
     * 入会密码
     */
    private String password;

    /**
     * 主持密码
     */
    private String moderatorPassword;

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

    /**
     * 允许呼入类型 0=不限制 
     */
    private Integer allowPart;

    /**
     * 允许录制  0/1
     */
    private Boolean allowRecord;

    private RoomStateEnum state;

    private RoomIdTypeEnums roomIdType;

    private static final long serialVersionUID = 1L;
}