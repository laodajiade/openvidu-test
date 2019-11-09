package io.openvidu.server.common.enums;

import lombok.Getter;

/**
 * @author geedow
 * @date 2019/10/16 17:50
 */
@Getter
public enum AuthorizationEnum {
    CREATE_CONFERENCE("createConference"),
    CONFERENCE_MANAGER("conferenceManager"),
    CONFERENCE_CONTROL("conferenceControl"),
    ORGANIZATION_MANAGER("organizationManager"),
    USER_MANAGER("userManager"),
    DEVICE_MANAGER("deviceManager"),
    ROLE_MANAGER("roleManager"),
    PARTICIPANT_ONLY("participantOnly");

    private String authorization;
    AuthorizationEnum(String authorization) {
        this.authorization = authorization;
    }}
