package io.openvidu.server.kurento.endpoint;

import lombok.Getter;
import lombok.Setter;
import org.kurento.client.PassThrough;
import org.kurento.client.WebRtcEndpoint;

public class DispatcherEndpoint {


    @Setter
    @Getter
    WebRtcEndpoint inEndpoint;

    @Setter
    @Getter
    WebRtcEndpoint outEndpoint;

    @Getter
    @Setter
    PassThrough passThrough;

    public DispatcherEndpoint(WebRtcEndpoint inEndpoint, WebRtcEndpoint outEndpoint,PassThrough passThrough) {
        this.inEndpoint = inEndpoint;
        this.outEndpoint = outEndpoint;
        this.passThrough = passThrough;
    }
}
