package io.openvidu.server.kurento.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.LayoutModeTypeEnum;
import io.openvidu.server.common.enums.RecordOutputMode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.kurento.client.MediaProfileSpecType;

/**
 * @author chosongi
 * @date 2020/9/8 16:23
 */
@Getter
@Setter
@Builder
public class ConferenceRecordingProperties {
    private String project;
    private String roomId;
    private String ruid;
    private Long startTime;
    private Long updateTime;
    private String rootPath;
    private RecordOutputMode outputMode;
    private MediaProfileSpecType mediaProfileSpecType;
    private Integer layoutMode;
    private LayoutModeTypeEnum layoutModeType;
    private JsonArray mediaSources;
    private JsonObject participantStatus;
}
