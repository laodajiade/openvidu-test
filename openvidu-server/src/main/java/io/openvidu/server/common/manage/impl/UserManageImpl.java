package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.RoleMapper;
import io.openvidu.server.common.dao.UserMapper;
import io.openvidu.server.common.dao.UserRoleMapper;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.manage.UserManage;
import io.openvidu.server.common.pojo.Role;
import io.openvidu.server.common.pojo.User;
import io.openvidu.server.common.pojo.dto.UserDeviceDeptInfo;
import io.openvidu.server.core.Participant;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserManageImpl implements UserManage {

    @Resource
    private CacheManage cacheManage;

    @Resource
    private DepartmentManage departmentManage;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleMapper roleMapper;

    @Resource
    private UserRoleMapper userRoleMapper;


    @Override
    public List<User> getSubUserByDeptId(Long deptId) {
        List<Long> subDeptIds = departmentManage.getSubDeptIds(deptId);
        if (CollectionUtils.isEmpty(subDeptIds)) return null;

        List<Long> userIds = userMapper.selectUserIdsByDeptIds(subDeptIds);
        return !CollectionUtils.isEmpty(userIds) ? userMapper.getUsersByUserIdsList(userIds) : null;
    }

    @Override
    public User queryByUuid(String uuid) {
        return userMapper.selectByUUID(uuid);
    }

    @Override
    public boolean isAdmin(String uuid) {
        User user = this.queryByUuid(uuid);
        if (Objects.isNull(user)) {
            return false;
        }
        if (Objects.equals("administrator", uuid)) {
            return true;
        }

        Long roleId = userRoleMapper.selectByUserId(user.getId());
        Role role = roleMapper.selectByPrimaryKey(roleId);

        return (Objects.nonNull(role) && Objects.equals(role.getRoleName(), "admin"));
    }

    @Override
    public Role getUserRoleById(Long roleId) {
        return roleMapper.selectByPrimaryKey(roleId);
    }

    @Override
    public User getUserByUserId(Long userId) {
        return userMapper.selectByPrimaryKey(userId);
    }

    @Override
    public int modifyPassword(User user) {
        User update = new User();
        update.setId(user.getId());
        update.setPassword(user.getPassword());

        return userMapper.updateByPrimaryKeySelective(update);
    }

    @Override
    public void updateUserInfo(User update) {
        String uuid = update.getUuid();
        update.setUuid(null);
        userMapper.updateByPrimaryKeySelective(update);
        cacheManage.updateTokenInfo(uuid, "username", update.getUsername());
    }

    @Override
    public Map<String, UserDeviceDeptInfo> getUserInfoInRoom(Set<Participant> participants) {
        Map<String, UserDeviceDeptInfo> connectIdPartMap = null;
        List<UserDeviceDeptInfo> userDeviceDeptInfos = userMapper.queryUserInfoByUserIds(participants.stream()
                .map(Participant::getUserId).collect(Collectors.toList()));
        if (!CollectionUtils.isEmpty(userDeviceDeptInfos)) {
            Map<Long, UserDeviceDeptInfo> userIdUserInfoMap = userDeviceDeptInfos.stream()
                    .collect(Collectors.toMap(UserDeviceDeptInfo::getUserId, Function.identity()));
            connectIdPartMap = participants.stream().filter(participant -> userIdUserInfoMap.containsKey(participant.getUserId()))
                    .collect(Collectors.toMap(Participant::getParticipantPublicId,
                            participant -> userIdUserInfoMap.get(participant.getUserId())));
        }
        return connectIdPartMap;
    }

    @Override
    public List<User> queryByUuidList(List<String> uuids) {
        return userMapper.selectUserByUuidList(uuids);
    }
}
