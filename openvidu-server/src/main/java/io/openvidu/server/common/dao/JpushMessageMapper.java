package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.JpushMessage;

/**
 * @author even
 * @date 2021/1/13 10:14
 */
public interface JpushMessageMapper {

    void insertMsg(JpushMessage jpushMessage);

}
