package io.openvidu.server.common.pojo;

import lombok.Builder;
import lombok.Data;

/**
 * @author even
 * @date 2021/1/13 17:02
 */
@Data
@Builder
public class JpushMsgTemp {

    private String ruid;

    private String date;

    private String msgType;

    private String title;

    private String content;
}
