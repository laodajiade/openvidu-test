package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.enums.TerminalTypeEnum;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Objects;

/**
 * @author even
 * @date 2020/8/28 17:02
 */
@Slf4j
@Service
public class AdjustToPresetPositionHandler extends RpcAbstractHandler {

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String serialNumber = getStringParam(request, ProtocolElements.ADJUSTTOPRESETPOSITION_SERIALNUMBER_PARAM);
        int index = getIntParam(request, ProtocolElements.ADJUSTTOPRESETPOSITION_INDEX_PARAM);
        String configInfo = getParam(request, ProtocolElements.ADJUSTTOPRESETPOSITION_CONFIGINFO_PARAM).toString();
        DeviceSearch search = new DeviceSearch();
        search.setSerialNumber(serialNumber);
        Device device = deviceMapper.selectBySearchCondition(search);
        if (Objects.isNull(device)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.DEVICE_NOT_FOUND);
            return;
        }
        RpcConnection deviceRpc = notificationService.getRpcConnections().stream().filter(s -> Objects.equals(s.getSerialNumber(), serialNumber)
                && Objects.equals(TerminalTypeEnum.HDC, s.getTerminalType())
        ).max(Comparator.comparing(RpcConnection::getCreateTime)).orElse(null);
        if (Objects.nonNull(deviceRpc)) {
            this.notificationService.sendNotification(deviceRpc.getParticipantPrivateId(),
                    ProtocolElements.ADJUSTTOPRESETPOSITIONNOTIFY_METHOD, request.getParams());
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
