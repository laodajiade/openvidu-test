package io.openvidu.server.rpc.handlers.parthistory;

import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.domain.vo.ConferenceHisResp;
import io.openvidu.server.domain.vo.GetConferenceScheduleVO;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service(ProtocolElements.GET_CONFERENCE_SCHEDULE_ADMIN_METHOD)
public class GetConferenceScheduleAdminHandler extends GetConferenceScheduleHandler {

    @Override
    protected List<ConferenceHisResp> pending(RpcConnection rpcConnection, GetConferenceScheduleVO params) {
        return new Pending(null, rpcConnection.getProject(), params).getList();
    }

}
