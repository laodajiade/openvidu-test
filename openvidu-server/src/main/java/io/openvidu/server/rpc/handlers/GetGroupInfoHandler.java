package io.openvidu.server.rpc.handlers;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonObject;
import io.openvidu.client.internal.ProtocolElements;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.pojo.UserDevice;
import io.openvidu.server.common.pojo.UserGroupVo;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.rpc.ExRpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author geedow
 * @date 2019/11/5 20:26
 */
@Slf4j
@Service
public class GetGroupInfoHandler extends ExRpcAbstractHandler<JsonObject> {

    @Override
    public RespResult<?> doProcess(RpcConnection rpcConnection, Request<JsonObject> request, JsonObject params) {
        Long groupId = getLongParam(request, ProtocolElements.GET_GROUP_INFO_GROUPID_PARAM);
        int pageNum = getIntParam(request, ProtocolElements.PAGENUM);
        int pageSize = getIntParam(request, ProtocolElements.PAGESIZE);
        List<Long> groupIds = new ArrayList<>();
        groupIds.add(groupId);
        PageHelper.startPage(pageNum, pageSize);
        List<UserGroupVo> userGroups = userGroupMapper.selectListByGroupid(groupIds);

        JSONObject resp = new JSONObject();
        if (!CollectionUtils.isEmpty(userGroups)) {
            for (UserGroupVo userGroupVo : userGroups) {
                if (userGroupVo.getType().equals(1)) {
                    UserDevice userDevSearch = new UserDevice();
                    userDevSearch.setUserId(userGroupVo.getUserId());
                    UserDevice userDevice = userDeviceMapper.selectByCondition(userDevSearch);
                    userGroupVo.setUsername(Objects.isNull(userDevice.getDeviceName()) ? null : userDevice.getDeviceName());
                    String deviceStatus = Objects.isNull(userDevice.getSerialNumber()) ? null : cacheManage.getDeviceStatus(userDevice.getSerialNumber());
                    userGroupVo.setStatus(Objects.isNull(deviceStatus) ? DeviceStatus.offline.name() : deviceStatus);
                } else {
                    userGroupVo.setStatus(cacheManage.getTerminalStatus(userGroupVo.getUuid()));
                }
            }
        }
        PageInfo<UserGroupVo> pageInfo = new PageInfo<>(userGroups);
        resp.put(ProtocolElements.PAGENUM, pageNum);
        resp.put(ProtocolElements.PAGESIZE, pageSize);
        resp.put(ProtocolElements.TOTAL, pageInfo.getTotal());
        resp.put(ProtocolElements.PAGES, pageInfo.getPages());
        resp.put(ProtocolElements.GET_GROUP_INFO_ACCOUNT_LIST, pageInfo.getList());
        return RespResult.ok(resp);
    }
}
