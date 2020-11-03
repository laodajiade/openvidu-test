package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.DeviceStatus;
import io.openvidu.server.common.manage.HiddenPhoneManage;
import io.openvidu.server.common.manage.HiddenSpecifyVisibleManage;
import io.openvidu.server.common.manage.HiddenUserHelper;
import io.openvidu.server.common.pojo.AllUserInfo;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.UserDept;
import io.openvidu.server.common.pojo.dto.SpecifyVisibleRule;
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

    @Autowired
    private HiddenPhoneManage hiddenPhoneManage;

    @Autowired
    private HiddenUserHelper hiddenUserHelper;

    @Autowired
    private HiddenSpecifyVisibleManage hiddenSpecifyVisibleManage;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {

        List<Long> deptIds = getLongListParam(request, "deptIds");
        List<String> uuids = getStringListParam(request, "uuids");

        SpecifyVisibleRule specifyVisibleRule = hiddenSpecifyVisibleManage.getSpecifyVisibleRule2(rpcConnection.getUserUuid(), rpcConnection.getUserId(), rpcConnection.getCorpId());
        // 全部隐藏，直接返回空列表
        if (specifyVisibleRule.getType() == 0) {
            JsonObject respJson = new JsonObject();
            respJson.add("list", new JsonArray());
            this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
            return;
        }

        List<AllUserInfo> list = new ArrayList<>();
        if (!CollectionUtils.isEmpty(deptIds)) {

            if (deptIds.contains(0L)) {
                Department rootDept = departmentManage.getRootDept(rpcConnection.getProject());
                deptIds.add(rootDept.getId());
            }

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

        Set<Long> notInUser = hiddenUserHelper.canNotVisible(rpcConnection.getUserId(), rpcConnection.getCorpId());
        list.removeIf(u -> notInUser.contains(u.getUserId()));

        // 仅可见
        if (specifyVisibleRule.getType() == 1) {
            list.removeIf(user -> !specifyVisibleRule.getVisibleUser().contains(user.getUserId()));
        }

        list.sort((u1, u2) -> (int) (u1.getUserId() - u2.getUserId()));

        hiddenPhoneManage.hiddenPhone2(list);

        JsonArray jsonArray = new JsonArray();
        list.forEach(e -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("uuid", e.getUuid());
            jsonObject.addProperty("userName", e.getUserName());
            jsonObject.addProperty("phone", e.getPhone());
            jsonObject.addProperty("type", e.getType());
            if (e.getType().equals(0)) {
                jsonObject.addProperty("status", cacheManage.getTerminalStatus(e.getUuid()));
            } else {
                String deviceStatus = Objects.isNull(e.getSerialNumber()) ? null : cacheManage.getDeviceStatus(e.getSerialNumber());
                jsonObject.addProperty("status", Objects.isNull(deviceStatus) ? DeviceStatus.offline.name() : deviceStatus);
            }
            jsonArray.add(jsonObject);
        });
        JsonObject respJson = new JsonObject();
        respJson.add("list", jsonArray);
        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), respJson);
    }
}
