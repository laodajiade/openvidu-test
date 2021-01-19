package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.CallHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author even
 * @date 2021/1/18 12:03
 */
public interface CallHistoryMapper {

    List<CallHistory> getCallHistoryList(String ruid);

    void insertBatch(List<CallHistory> list);

    void updateCallHistory(@Param("ruid") String ruid, @Param("uuid") String uuid);
}
