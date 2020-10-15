package io.openvidu.server.common.manage;

import io.openvidu.server.common.pojo.AppointConference;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.domain.vo.AppointmentRoomVO;
import io.openvidu.server.rpc.RpcConnection;
import org.kurento.jsonrpc.message.Request;

public interface AppointConferenceManage {

    boolean isConflict(AppointmentRoomVO vo);

    Conference constructConf(RpcConnection rpcConnection, Request<AppointmentRoomVO> request);

    void insert(AppointmentRoomVO params, RpcConnection rpcConnection);

    AppointConference getByRuid(String ruid);
}
