package io.openvidu.server.kurento.mcu;

import io.openvidu.server.common.enums.StreamType;
import io.openvidu.server.core.Participant;
import io.openvidu.server.kurento.endpoint.PublisherEndpoint;

import java.util.Objects;

public class CompositeObjectWrapper {
    public String uuid;
    public String username;
    public int order;
    public StreamType streamType;
    public String streamId;
    public PublisherEndpoint endpoint;
    public boolean isStreaming = false;

    public CompositeObjectWrapper(Participant participant, StreamType streamType, PublisherEndpoint endpoint) {
        this.uuid = participant.getUuid();
        this.username = participant.getUsername();
        this.order = participant.getOrder();
        this.streamType = streamType;
        this.endpoint = endpoint;
        if (endpoint != null) {
            this.streamId = endpoint.getStreamId();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompositeObjectWrapper that = (CompositeObjectWrapper) o;
        return order == that.order &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(username, that.username) &&
                streamType == that.streamType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, username, order, isStreaming, streamType);
    }

    @Override
    public String toString() {
        return "CompositeObject{" +
                "uuid='" + uuid + '\'' +
                ", username='" + username + '\'' +
                ", streamType=" + streamType +
                ", isStreaming=" + isStreaming +
                ", endpoint=" + (endpoint == null ? "null" : endpoint.getStreamId()) +
                '}';
    }
}
