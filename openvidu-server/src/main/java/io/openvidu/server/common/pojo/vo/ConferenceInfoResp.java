package io.openvidu.server.common.pojo.vo;

import com.google.gson.annotations.JsonAdapter;
import io.openvidu.server.utils.LocalDateTimeAdapter;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author
 */
@Data
public class ConferenceInfoResp implements Serializable {

    private String roomId;

    /**
     * 会议室名称
     */
    private String conferenceSubject;

    private String ruid;

    /**
     * 到期时间
     */
    @JsonAdapter(LocalDateTimeAdapter.class)
    private LocalDateTime startTime;


    private String moderatorUuid;

    private String moderatorName;

    private static final long serialVersionUID = 1L;
}