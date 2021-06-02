package io.openvidu.server.common.enums;

import lombok.Getter;

/**
 * @author Administrator
 */

@Getter
public enum ArtisanEnum {
    //当前支持的录制数
    RECORDING_NUM(1);

    private Integer value;


    ArtisanEnum(Integer value) {
        this.value = value;
    }
}
