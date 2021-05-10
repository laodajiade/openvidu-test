package io.openvidu.server.config;

import io.openvidu.server.common.enums.DeployTypeEnum;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EnvConfig {

    public DeployTypeEnum deployType = DeployTypeEnum.LOCAL;
}
