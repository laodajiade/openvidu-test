package io.openvidu.server.domain;

import lombok.Getter;
import lombok.Setter;
import org.kurento.jsonrpc.message.Request;

public class RequestEx<P>  extends Request<P> {
    @Setter
    @Getter
    private String trackId;
}
