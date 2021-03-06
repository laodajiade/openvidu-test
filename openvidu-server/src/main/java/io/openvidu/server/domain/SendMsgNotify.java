package io.openvidu.server.domain;

import io.openvidu.server.domain.vo.SendMsgVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SendMsgNotify extends SendMsgVO {

    private Long msgId;

    private ImUser sender;

    private List<ImUser> recivers;

}


