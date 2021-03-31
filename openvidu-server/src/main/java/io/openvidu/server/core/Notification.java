package io.openvidu.server.core;

import io.openvidu.server.common.enums.StreamType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Setter
@Getter
@ToString
public class Notification {

    private String method;
    private Object params;

    private List<String> participantIds;

    public Notification(String method) {
        this.method = method;
    }

    public Notification(String method, Object params) {
        this.method = method;
        this.params = params;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public void setParticipantIds(String... participantIds) {
        this.participantIds = Arrays.asList(participantIds);
    }


    public void withParticipantIds(String roomId, SessionManager sessionManager) {
        participantIds = sessionManager.getParticipants(roomId).stream().filter(p -> Objects.equals(StreamType.MAJOR, p.getStreamType()))
                .map(Participant::getParticipantPrivateId).collect(Collectors.toList());
    }
}
