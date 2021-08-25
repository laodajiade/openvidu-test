package io.openvidu.server.config;

import io.openvidu.server.common.enums.DeployTypeEnum;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EnvConfig {

    public DeployTypeEnum deployType = DeployTypeEnum.LOCAL;

    /**
     * 是否是基线的运行环境，反之是私有化部署
     */
    public boolean isSassDeployEnv() {
        return deployType == DeployTypeEnum.SASS;
    }
}
