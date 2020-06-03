package io.openvidu.server.common.pojo;

import lombok.Data;

@Data
public class KmsRegistration {
    private String kmsUri;

    private String region;
}