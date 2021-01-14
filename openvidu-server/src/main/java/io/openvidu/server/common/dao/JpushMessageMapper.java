package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.JpushMessage;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author even
 * @date 2021/1/13 10:14
 */
public interface JpushMessageMapper {

    void insertMsg(JpushMessage jpushMessage);

    List<JpushMessage> getJpushMsgList(String uuid);

    void updateJpushMsg(@Param("readType") Integer readType, @Param("uuid") String uuid, @Param("list") List<Long> ids);

    void delJpushMsg(String uuid);

    int getNotReadMsgCount(String uuid);

}
