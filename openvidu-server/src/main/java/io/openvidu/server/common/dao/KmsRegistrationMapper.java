package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.KmsRegistration;

import java.util.List;

public interface KmsRegistrationMapper {

    List<KmsRegistration> selectAllRegisterKms();

}