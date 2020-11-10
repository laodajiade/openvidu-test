package io.openvidu.server.rpc.handlers.parthistory;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.AppointConferenceMapper;
import io.openvidu.server.common.dao.ConferenceMapper;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.core.PageResult;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.domain.vo.ConferenceHisResp;
import io.openvidu.server.domain.vo.GetConferenceScheduleVO;
import io.openvidu.server.domain.vo.PartHisResp;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service(ProtocolElements.GET_PARTICIPATE_HISTORY_METHOD)
public class GetParticipateHistoryHandler extends ExRpcAbstractHandler<GetConferenceScheduleVO> {

    @Resource
    private ConferencePartHistoryMapper conferencePartHistoryMapper;


    @Override
    public RespResult<PageResult<PartHisResp>> doProcess(RpcConnection rpcConnection, Request<GetConferenceScheduleVO> request,
                                                         GetConferenceScheduleVO params) {
        BindValidate.notNull(params::getPageNum);
        BindValidate.notNull(params::getPageSize);

        Page<Object> page = PageHelper.startPage(params.getPageNum(), params.getPageSize());

        List<PartHisResp> partHisResps = conferencePartHistoryMapper.selectUserHistory(rpcConnection.getUserId());

        return RespResult.ok(new PageResult<>(partHisResps, page));
    }
}
