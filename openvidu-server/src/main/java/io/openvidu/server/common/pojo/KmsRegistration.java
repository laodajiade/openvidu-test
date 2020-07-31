package io.openvidu.server.common.pojo;

import lombok.Data;

@Data
public class KmsRegistration {
    // 媒体服务uri
    private String kmsUri;
    // 区域
    private String region;
}