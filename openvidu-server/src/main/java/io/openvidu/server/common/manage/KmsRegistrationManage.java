package io.openvidu.server.common.manage;

import java.util.List;

/**
 * @author chosongi
 * @date 2020/6/3 10:31
 */
public interface KmsRegistrationManage {
    List<String> getAllRegisterKms() throws Exception;

    List<String> getAllDeliveryKms() throws Exception;

    List<String> getRecentRegisterKms();
}
