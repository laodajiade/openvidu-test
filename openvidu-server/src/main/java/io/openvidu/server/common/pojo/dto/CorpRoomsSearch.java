package io.openvidu.server.common.pojo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author chosongi
 * @date 2020/9/27 11:45
 */

@Data
@Builder
public class CorpRoomsSearch {
    private String project;
    private String roomId;
    private List<Long> limitDept;
}
