package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.dao.OftenContactsMapper;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.manage.HiddenPhoneManage;
import io.openvidu.server.common.manage.HiddenSpecifyVisibleManage;
import io.openvidu.server.common.manage.HiddenUserHelper;
import io.openvidu.server.common.pojo.UserDevice;
import io.openvidu.server.common.pojo.dto.HiddenSpecifyVisibleDTO;
import io.openvidu.server.common.pojo.vo.OftenContactsVo;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * 查询常用联系人列表
 * @author Administrator
 */
@Service
@Slf4j
public class GetFrequentContactsHandler extends RpcAbstractHandler {

    @Resource
    private OftenContactsMapper oftenContactsMapper;
    @Resource
    private HiddenSpecifyVisibleManage hiddenSpecifyVisibleManage;
    @Resource
    private HiddenPhoneManage hiddenPhoneManage;
    @Resource
    private HiddenUserHelper hiddenUserHelper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        boolean isChooseAll = getBooleanOptionalParam(request, "isChooseAll");
        int pageNum = isChooseAll ? 1 : getIntParam(request, ProtocolElements.PAGENUM);
        int pageSize = isChooseAll ? Integer.MAX_VALUE : getIntParam(request, ProtocolElements.PAGESIZE);

        JSONObject resp = new JSONObject();
        resp.put(ProtocolElements.PAGENUM, pageNum);
        resp.put(ProtocolElements.PAGESIZE, pageSize);

        HiddenSpecifyVisibleDTO specifyVisibleRule = hiddenSpecifyVisibleManage.getSpecifyVisibleRule(rpcConnection.getUserUuid(),
                rpcConnection.getUserId(), rpcConnection.getCorpId());
        // 全部隐藏，直接返回空列表
        if (specifyVisibleRule.getType() == 0) {
            resp.put(ProtocolElements.TOTAL, 0);
            resp.put(ProtocolElements.PAGES, 0);
            resp.put(ProtocolElements.GET_GROUP_INFO_ACCOUNT_LIST, new ArrayList<>(0));
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), resp);
        }
        Set<Long> notInUser = hiddenUserHelper.canNotVisible(rpcConnection.getUserId(), rpcConnection.getCorpId());
        PageHelper.startPage(pageNum, pageSize);

        Map<String ,Object> map = new HashMap<>();
        map.put("userId",rpcConnection.getUserId());
        map.put("list",notInUser);

        List<OftenContactsVo> oftenContactsList = oftenContactsMapper.getOftenContactsList(map);

        if (!CollectionUtils.isEmpty(oftenContactsList)) {
            for (OftenContactsVo oftenContactsVo : oftenContactsList) {
                if (oftenContactsVo.getAccountType().equals(1)) {
                    UserDevice userDevSearch = new UserDevice();
                    userDevSearch.setUserId(oftenContactsVo.getId());
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

        hiddenPhoneManage.hiddenContactsPhone(oftenContactsList);
        PageInfo<OftenContactsVo> pageInfo = new PageInfo<>(oftenContactsList);
        resp.put(ProtocolElements.TOTAL, pageInfo.getTotal());
        resp.put(ProtocolElements.PAGES, pageInfo.getPages());
        resp.put(ProtocolElements.GET_GROUP_INFO_ACCOUNT_LIST, pageInfo.getList());

        notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), resp);
    }
}
