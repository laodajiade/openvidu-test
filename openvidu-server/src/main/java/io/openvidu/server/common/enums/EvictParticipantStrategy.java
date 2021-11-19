package io.openvidu.server.common.enums;

/**
 * @author chosongi
 * @date 2020/8/17 15:01
 */
public enum EvictParticipantStrategy {
    CLOSE_WEBSOCKET_CONNECTION,
    CLOSE_ROOM_WHEN_EVICT_MODERATOR,
    LAST_PARTICIPANT_LEFT
}
