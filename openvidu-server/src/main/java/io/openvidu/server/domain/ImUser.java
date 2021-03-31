package io.openvidu.server.domain;

import lombok.Data;

@Data
public class ImUser {

    public ImUser() {
    }

    public ImUser(String uuid, String username, String terminalType) {
        this.uuid = uuid;
        this.username = username;
        this.terminalType = terminalType;
    }

    private String uuid;
    private String username;
    private String terminalType;

}
