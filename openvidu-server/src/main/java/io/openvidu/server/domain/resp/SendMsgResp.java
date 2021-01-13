package io.openvidu.server.domain.resp;

import lombok.Data;

@Data
public class SendMsgResp {
    private long msgId;

    private String clientMsgId;

    private String ruid;

    private String roomId;

    private long timestamp;
}
