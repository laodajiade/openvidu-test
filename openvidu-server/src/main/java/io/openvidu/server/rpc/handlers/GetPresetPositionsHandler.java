package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.common.pojo.Preset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author even
 * @date 2020/8/31 10:37
 */
@Slf4j
@Service
public class GetPresetPositionsHandler extends RpcAbstractHandler {

    @Value("${preset.thumbnail.url}")
    private String thumbnailUrl;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String serialNumber = getStringParam(request, ProtocolElements.GETPRESETPOSITIONS_SERIALNUMBER_PARAM);
        DeviceSearch search = new DeviceSearch();
        search.setSerialNumber(serialNumber);
        Device device = deviceMapper.selectBySearchCondition(search);
        if (Objects.isNull(device)) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.DEVICE_NOT_FOUND);
            return;
        }
        List<Preset> presetList = presetMapper.selectList(serialNumber);
        JsonArray jsonArray = new JsonArray();
        if (!CollectionUtils.isEmpty(presetList)) {
            presetList.forEach(preset -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("index",preset.getIndex());
                jsonObject.addProperty("configInfo",preset.getConfigInfo());
                jsonObject.addProperty("thumbnailUrl",thumbnailUrl + preset.getThumbnailUrl());
                jsonArray.add(jsonObject);
            });
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(),jsonArray);
    }
}
