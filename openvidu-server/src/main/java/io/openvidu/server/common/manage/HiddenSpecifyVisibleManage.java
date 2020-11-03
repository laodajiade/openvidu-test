package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.dto.HiddenSpecifyVisibleDTO;
import io.openvidu.server.common.pojo.dto.SpecifyVisibleRule;
import org.springframework.stereotype.Service;

public interface HiddenSpecifyVisibleManage {


    HiddenSpecifyVisibleDTO getSpecifyVisibleRule(String uuid, Long userId, Long corpId);


    SpecifyVisibleRule getSpecifyVisibleRule2(String uuid, Long userId, Long corpId);
}
