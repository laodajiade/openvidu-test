package io.openvidu.server.common.pojo;

import lombok.Data;

import java.util.List;

/**
 * @author even
 * @date 2021/1/20 19:33
 */
@Data
public class ScrollingBannersConfig {

    private String content;

    private String position;

    private String displayMode;

    private String fontSize;

    private String bgColor;

    private String opacity;

    List<UserDto> targetIds;
}
