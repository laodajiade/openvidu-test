package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.OftenContactsMapper;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.pojo.UserDevice;
import io.openvidu.server.common.pojo.vo.OftenContactsVo;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 查询常用联系人列表
 * @author Administrator
 */
@Service
@Slf4j
public class GetFrequentContactsHandler extends RpcAbstractHandler {

    @Resource
    private OftenContactsMapper oftenContactsMapper;


    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        boolean isChooseAll = getBooleanOptionalParam(request, "isChooseAll");
        int pageNum = isChooseAll ? 1 : getIntParam(request, ProtocolElements.PAGENUM);
        int pageSize = isChooseAll ? Integer.MAX_VALUE : getIntParam(request, ProtocolElements.PAGESIZE);
        PageHelper.startPage(pageNum, pageSize);
        JSONObject resp = new JSONObject();
        List<OftenContactsVo> oftenContactsList = oftenContactsMapper.getOftenContactsList(rpcConnection.getUserId());
        if (!CollectionUtils.isEmpty(oftenContactsList)) {
            for (OftenContactsVo oftenContactsVo : oftenContactsList) {
                if (oftenContactsVo.getAccountType().equals(1)) {
                    UserDevice userDevSearch = new UserDevice();
                    userDevSearch.setUserId(oftenContactsVo.getUserId());
                    UserDevice userDevice = userDeviceMapper.selectByCondition(userDevSearch);
                    oftenContactsVo.setUserName(Objects.isNull(userDevice.getDeviceName()) ? null : userDevice.getDeviceName());
                    String deviceStatus = Objects.isNull(userDevice.getSerialNumber()) ? null : cacheManage.getDeviceStatus(userDevice.getSerialNumber());
                    oftenContactsVo.setStatus(Objects.isNull(deviceStatus) ? DeviceStatus.offline.name() : deviceStatus);
                } else {
                    oftenContactsVo.setStatus(cacheManage.getTerminalStatus(oftenContactsVo.getUuid()));
                }
                oftenContactsVo.setAccountType(Integer.valueOf(oftenContactsVo.getAccountType()) >= 1 ? 1 : 0);
            }
        }
        PageInfo<OftenContactsVo> pageInfo = new PageInfo<>(oftenContactsList);
        resp.put(ProtocolElements.PAGENUM, pageNum);
        resp.put(ProtocolElements.PAGESIZE, pageSize);
        resp.put(ProtocolElements.TOTAL, pageInfo.getTotal());
        resp.put(ProtocolElements.PAGES, pageInfo.getPages());
        resp.put(ProtocolElements.GET_GROUP_INFO_ACCOUNT_LIST, pageInfo.getList());
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), resp);
    }
}
