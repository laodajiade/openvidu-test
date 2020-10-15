package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author even
 * @date 2020/8/28 14:20
 */
@Slf4j
@Service
public class DiscardPresetPositionsHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String serialNumber = getStringParam(request, ProtocolElements.DISCARDPRESETPOSITIONS_SERIALNUMBER_PARAM);
        List<Long> indexs = getLongListParam(request, ProtocolElements.DISCARDPRESETPOSITIONS_INDEXARRAY_PARAM);
        if (CollectionUtils.isEmpty(indexs)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
            return;
        }

        DeviceSearch search = new DeviceSearch();
        search.setSerialNumber(serialNumber);
        Device device = deviceMapper.selectBySearchCondition(search);
        if (Objects.isNull(device)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.DEVICE_NOT_FOUND);
            return;
        }
        presetMapper.deleteByIndexs(serialNumber,indexs);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
