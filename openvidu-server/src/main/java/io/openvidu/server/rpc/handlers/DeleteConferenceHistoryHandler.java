package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.ConferencePartHistoryMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Conference;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Objects;

@Service(ProtocolElements.DELETE_CONFERENCE_HISTORY_METHOD)
public class DeleteConferenceHistoryHandler extends ExRpcAbstractHandler<JsonObject> {


    @Resource
    private ConferencePartHistoryMapper conferencePartHistoryMapper;

    @Transactional
    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        BindValidate.notEmpty(params, "ruid");

        String ruid = getStringParam(request, "ruid");

        Conference conference = conferenceMapper.selectByRuid(ruid);
        if (conference == null) {
            return RespResult.fail(ErrorCodeEnum.CONFERENCE_NOT_EXIST);
        }

        if (conference.getStatus() < 2) {
            return RespResult.fail(ErrorCodeEnum.PERMISSION_LIMITED);
        }

        if (Objects.equals(conference.getUserId(), rpcConnection.getUserId())) {
            conferenceMapper.softDeleteById(conference.getId());
        }

        conferencePartHistoryMapper.softDelete(ruid, rpcConnection.getUserId());
        return RespResult.ok();
    }
}
