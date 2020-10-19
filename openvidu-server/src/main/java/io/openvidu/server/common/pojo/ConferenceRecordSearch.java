package io.openvidu.server.common.pojo;

import io.openvidu.server.common.enums.SortEnum;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Data
@Builder
public class ConferenceRecordSearch {
    private List<String> ruidList;
    private Integer recordQueryTimeInterval;
    private int limit;
    private long offset;

    private String project;
    private String roomId;
    private String roomSubject;
    private int size;
    private int pageNum;
    private SortEnum sort;
    private SortFilter filter;
    private List<String> roomIds;

    public enum SortFilter {
        startTime("start_time"),
        duration("duration"),
        occupation("record_size");

        @Getter
        private String filter;

        SortFilter(String filter) {
            this.filter = filter;
        }
    }
}
