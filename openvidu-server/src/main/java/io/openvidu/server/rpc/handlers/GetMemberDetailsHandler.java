package io.openvidu.server.rpc.handlers;

import com.google.gson.JsonObject;
import io.openvidu.server.common.dao.OftenContactsMapper;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.common.pojo.Department;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.UserDept;
import io.openvidu.server.rpc.RpcAbstractHandler;
import io.openvidu.server.rpc.RpcConnection;
import io.openvidu.server.service.DepartmentService;
import io.openvidu.server.service.UserDeptService;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Service
public class GetMemberDetailsHandler extends RpcAbstractHandler {

    @Autowired
    private UserDeptService userDeptService;

    @Autowired
    private DepartmentService departmentService;

    @Resource
    private OftenContactsMapper oftenContactsMapper;

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        String uuid = getStringParam(request, "uuid");
        User user = userManage.queryByUuid(uuid);

        if (user == null) {
            this.notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.USER_NOT_EXIST);
            return;
        }

        UserDept userDept = userDeptService.getByUserId(user.getId());
        Map<String,Object> map = new HashMap<>();
        map.put("uuid",uuid);
        map.put("userId",rpcConnection.getUserId());
        final boolean isFrequentContact = oftenContactsMapper.isOftenContacts(map);
        long deptId = userDept.getDeptId();
        String deptPath = "";
        do {
            Department department = departmentService.getById(deptId);

            if (deptPath.length() == 0) {
                deptPath = department.getDeptName();
            } else {
                deptPath = department.getDeptName() + "/" + deptPath;
            }
            deptId = department.getParentId() == null ? 0L : department.getParentId();
        } while (deptId != 0L);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("uuid",user.getUuid());
        jsonObject.addProperty("userName",user.getUsername());
        jsonObject.addProperty("userIcon",user.getIcon());
        jsonObject.addProperty("phone",user.getPhone());
        jsonObject.addProperty("email",user.getEmail());
        jsonObject.addProperty("department",deptPath);
        jsonObject.addProperty("isFrequentContact",isFrequentContact);

        this.notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), jsonObject);
    }
}
