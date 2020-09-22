package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.pojo.AllUserInfo;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.UserDept;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.service.UserDeptService;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author even
 * @date 2020/9/22 14:48
 */
@Slf4j
@Service
public class RecursiveQueryUserHandler extends RpcAbstractHandler {

    @Autowired
    private UserDeptService userDeptService;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {

        List<Long> deptIds = getLongListParam(request,"deptIds");
        List<String> uuids = getStringListParam(request,"uuids");
        List<AllUserInfo> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(deptIds)) {
            List<Long> allChildDeptIds = departmentManage.getAllChildDept(deptIds);
            List<UserDept> userDepts = userDeptService.getByDeptIds(allChildDeptIds);

            if (!userDepts.isEmpty()) {
                List<Long> userIds = userDepts.stream().map(UserDept::getUserId).collect(Collectors.toList());
                List<AllUserInfo> users = userMapper.selectAllUserByUserIdsList(userIds);
                list.addAll(users);
            }
        }

        if (!CollectionUtils.isEmpty(uuids)) {
            List<AllUserInfo> users = userMapper.selectAllUserByUuidList(uuids);
            // 去重
            if (list.isEmpty()) {
                list.addAll(users);
            } else {
                Set<String> uuidSet = list.stream().map(AllUserInfo::getUuid).collect(Collectors.toSet());
                users.removeIf(su -> uuidSet.contains(su.getUuid()));
                list.addAll(users);
            }
        }

        list.sort((u1, u2) -> (int) (u1.getUserId() - u2.getUserId()));
        JsonArray jsonArray = new JsonArray();
        list.forEach(e -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("uuid",e.getUuid());
            jsonObject.addProperty("userName",e.getUserName());
            jsonObject.addProperty("phone",e.getPhone());
            jsonObject.addProperty("type",e.getType());
            if (e.getType().equals(0)) {
                jsonObject.addProperty("status",cacheManage.getTerminalStatus(e.getUuid()));
            } else {
                String deviceStatus = Objects.isNull(e.getSerialNumber()) ? null : cacheManage.getDeviceStatus(e.getSerialNumber());
                jsonObject.addProperty("status",Objects.isNull(deviceStatus) ? DeviceStatus.offline.name() : deviceStatus);
            }
            jsonArray.add(jsonObject);
        });
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonArray);
    }
}
