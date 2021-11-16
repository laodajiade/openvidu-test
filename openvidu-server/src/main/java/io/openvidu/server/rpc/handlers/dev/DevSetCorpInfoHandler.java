package io.openvidu.server.rpc.handlers.dev;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.BindValidate;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 调试接口，修改企业属性
 */
@Slf4j
@Service(ProtocolElements.DEV_SET_CORP_INFO_MOTHOD)
public class DevSetCorpInfoHandler extends ExRpcAbstractHandler<JsonObject> {

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        String pwd = BindValidate.notEmptyAndGet(params, "pwd");

        if (!Objects.equals(pwd, "sudi123")) {
            return RespResult.ok();
        }
        if (params.has("sfuPublisherThreshold")) {
            final Corporation corporation = corporationMapper.selectByCorpProject(rpcConnection.getProject());
            corporation.setSfuPublisherThreshold(params.get("sfuPublisherThreshold").getAsInt());
            corporationMapper.updateOtherByPrimaryKey(corporation);
        }

        return RespResult.ok();
    }
}
