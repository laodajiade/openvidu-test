package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import io.openvidu.server.common.pojo.Preset;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author even
 * @date 2020/8/28 11:30
 */
@Slf4j
@Service
public class SetPresetPositionHandler extends RpcAbstractHandler {

    @Value("${preset.thumbnail.path}")
    private String thumbnailPath;
    @Value("${preset.thumbnail.url}")
    private String thumbnailUrl;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String serialNumber = getStringParam(request, ProtocolElements.SET_PRESET_POSITION_SERIALNUMBER_PARAM);
        int index = getIntParam(request, ProtocolElements.SET_PRESET_POSITION_INDEX_PARAM);
        String configInfo = getParam(request, ProtocolElements.SET_PRESET_POSITION_CONFIGINFO_PARAM).toString();
        String thumbnail = getStringParam(request, ProtocolElements.SET_PRESET_POSITION_THUMBNAIL_PARAM);
        String imgUrl = "";
        try {
            DeviceSearch search = new DeviceSearch();
            search.setSerialNumber(serialNumber);
            Device device = deviceMapper.selectBySearchCondition(search);
            if (Objects.isNull(device)) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.DEVICE_NOT_FOUND);
                return;
            }
            Preset presetInfo = presetMapper.selectBySerialNumberAndIndex(serialNumber,index);
            imgUrl = StringUtil.uploadBase64Image(thumbnail,serialNumber,thumbnailPath,thumbnailUrl);
            if (Objects.nonNull(presetInfo)) {
                presetInfo.setThumbnailUrl(imgUrl);
                presetInfo.setConfigInfo(StringUtils.isEmpty(configInfo) ? null : configInfo);
                presetMapper.update(presetInfo);
            } else {
                Preset preset = new Preset();
                preset.setSerialNumber(serialNumber);
                preset.setIndex(index);
                preset.setConfigInfo(configInfo);
                preset.setThumbnailUrl(imgUrl);
                presetMapper.insert(preset);
            }
        } catch (Exception e) {
            log.error("新增预置点异常,异常信息:",e);
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.SERVER_UNKNOWN_ERROR);
            return;
        }
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), new JsonObject());
    }
}
