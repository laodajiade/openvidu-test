package io.openvidu.server.common.pojo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConferenceRecordSearch {
    private List<String> ruidList;
    private Integer recordQueryTimeInterval;
    private int limit;
    private long offset;
}
