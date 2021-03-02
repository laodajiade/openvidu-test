package io.openvidu.server.kurento.endpoint;

import io.openvidu.server.config.OpenviduConfig;
import io.openvidu.server.kurento.core.KurentoParticipant;
import org.kurento.client.MediaPipeline;

public class PublisherDeliveryEndpoint extends PublisherEndpoint {

    public PublisherDeliveryEndpoint(boolean web, KurentoParticipant owner, String endpointName,
                                     MediaPipeline pipeline, OpenviduConfig openviduConfig) {
        super(web, owner, endpointName, pipeline, openviduConfig);
    }
}
