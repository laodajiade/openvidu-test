package io.openvidu.server.rpc.handlers;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.AllUserInfo;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;


@Slf4j
@Service
public class GetSpecificPageOfMemberHandler extends RpcAbstractHandler {


    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        boolean isChooseAll = getBooleanParam(request,"isChooseAll");
        Long deptId = getLongParam(request,"deptId");
        Integer pageNum = getIntOptionalParam(request,"pageNum");
        Integer pageSize = getIntOptionalParam(request,"pageSize");

        if (isChooseAll) {
            PageHelper.startPage(1,Integer.MAX_VALUE);
        } else {
            if (Objects.isNull(pageNum) || Objects.isNull(pageSize)) {
                this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
                return;
            }
            PageHelper.startPage(pageNum,pageSize);
        }
        List<AllUserInfo> allUserInfos = userMapper.selectAllUserList(deptId);
        JsonArray jsonArray = new JsonArray();
        if (!CollectionUtils.isEmpty(allUserInfos)) {
            allUserInfos.forEach(e -> {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("uuid",e.getUuid());
                jsonObject.addProperty("userName",e.getUserName());
                jsonObject.addProperty("phone",e.getPhone());
                if (e.getType().equals(0)) {
                    jsonObject.addProperty("status",cacheManage.getTerminalStatus(e.getUuid()));
                } else {
                    String deviceStatus = Objects.isNull(e.getSerialNumber()) ? null : cacheManage.getDeviceStatus(e.getSerialNumber());
                    jsonObject.addProperty("status",Objects.isNull(deviceStatus) ? DeviceStatus.offline.name() : deviceStatus);
                }
                jsonObject.addProperty("type",e.getType());
                jsonArray.add(jsonObject);
            });
        }
        PageInfo<AllUserInfo> pageInfo = new PageInfo<>(allUserInfos);
        JsonObject respJson = new JsonObject();
        respJson.addProperty("total",pageInfo.getTotal());
        respJson.addProperty("pageNum",pageInfo.getPageNum());
        respJson.addProperty("pageSize",pageInfo.getPageSize());
        respJson.addProperty("pages",pageInfo.getPages());
        respJson.add("list",jsonArray);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}
