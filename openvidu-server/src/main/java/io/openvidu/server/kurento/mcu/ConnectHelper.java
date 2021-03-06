package io.openvidu.server.kurento.mcu;

import io.openvidu.server.kurento.endpoint.PublisherEndpoint;
import io.openvidu.server.kurento.endpoint.SubscriberEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.Continuation;
import org.kurento.client.HubPort;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaType;

@Slf4j
public class ConnectHelper {
    /**
     * SIP特殊处理逻辑
     */
    public void sipConnect(PublisherEndpoint publisher) {

    }

    public static void connectVideoHubAndAudioHub(final HubPort videoHubPort, final HubPort audioHubPort,
                                                  final MediaElement sink, String sinkEndpointName) {
        disconnect(videoHubPort, sink, MediaType.AUDIO, sinkEndpointName);
        connect(videoHubPort, sink, MediaType.VIDEO, sinkEndpointName);
        connect(audioHubPort, sink, MediaType.AUDIO, sinkEndpointName);
    }

    /**
     * 只混音频，不混视频。并且在MCU模式下
     *
     * @param videoHubPort
     * @param audioHubPort
     * @param sink
     * @param sinkEndpointName
     */
    public static void connectOnlyAudio(final HubPort videoHubPort, final HubPort audioHubPort,
                                        final MediaElement sink, String sinkEndpointName) {
        disconnect(videoHubPort, sink, MediaType.AUDIO, sinkEndpointName);
        connect(videoHubPort, sink, MediaType.VIDEO, sinkEndpointName);
        connect(audioHubPort, sink, MediaType.AUDIO, sinkEndpointName);
    }


    public static void connect(final HubPort hubPort, final SubscriberEndpoint subscriberEndpoint) {
        hubPort.connect(subscriberEndpoint.getEndpoint(), new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) {
                log.info("MCU subscribe {}: Elements have been connected (source {} -> sink {})", subscriberEndpoint.getStreamId(),
                        hubPort.getId(), subscriberEndpoint.getEndpoint().getId());
            }

            @Override
            public void onError(Throwable cause) {
                log.warn("MCU subscribe {}: Failed to connect media elements (source {} -> sink {})", subscriberEndpoint.getStreamId(),
                        hubPort.getId(), subscriberEndpoint.getEndpoint().getId(), cause);
            }
        });
    }


    public static void connect(final HubPort source, final MediaElement sink, final MediaType mediaType, String sinkEndpointName) {
        source.connect(sink, mediaType, new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) {
                log.info("MCU subscribe {} {}: Elements have been connected (source {} -> sink {})", sinkEndpointName, mediaType.name(),
                        source.getId(), sink.getId());
            }

            @Override
            public void onError(Throwable cause) {
                log.warn("MCU subscribe {} {}: Failed to connect media elements (source {} -> sink {})", sinkEndpointName, mediaType.name(),
                        source.getId(), sink.getId(), cause);
            }
        });
    }

    //**********************************************************************************

    public static void disconnect(final HubPort source, final MediaElement sink, final MediaType type, String sinkEndPointName) {
        if (type == null) {
            ConnectHelper.disconnect(source, sink, sinkEndPointName);
        } else {
            source.disconnect(sink, type, new Continuation<Void>() {
                @Override
                public void onSuccess(Void result) {
                    log.info("MCU subscribe {}: {} media elements have been disconnected (source {} -> sink {})",
                            sinkEndPointName, type, source.getId(), sink.getId());
                }

                @Override
                public void onError(Throwable cause) {
                    log.info("MCU subscribe {}: Failed to disconnect {} media elements (source {} -> sink {})", sinkEndPointName,
                            type, source.getId(), sink.getId(), cause);
                }
            });
        }
    }

    private static void disconnect(final MediaElement source, final MediaElement sink, String sinkEndPointName) {
        source.disconnect(sink, new Continuation<Void>() {
            @Override
            public void onSuccess(Void result) {
                log.debug("MCU subscribe {}: Elements have been disconnected (source {} -> sink {})", sinkEndPointName,
                        source.getId(), sink.getName());
            }

            @Override
            public void onError(Throwable cause) {
                log.warn("MCU subscribe {}: Failed to disconnect media elements (source {} -> sink {})", sinkEndPointName,
                        source.getId(), sink.getName(), cause);
            }
        });
    }


}
