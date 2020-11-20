package io.openvidu.server.common.manage;

import io.openvidu.server.common.dao.HiddenUserGroupMapper;
import io.openvidu.server.common.dao.HiddenUserMapper;
import io.openvidu.server.common.dao.HiddenUserVisibleMapper;
import io.openvidu.server.common.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HiddenUserHelper {

    @Autowired
    private HiddenUserGroupMapper hiddenUserGroupMapper;

    @Autowired
    private HiddenUserVisibleMapper hiddenUserVisibleMapper;

    @Autowired
    private HiddenUserMapper hiddenUserMapper;


    /**
     * 返回隐藏的userId的列表
     */
    public Set<Long> canNotVisible(Long userId, Long corpId) {

        List<HiddenUserGroup> hiddenUserGroups = this.listByCorpId(corpId);

        Set<Long> notIn = new HashSet<>();
        for (HiddenUserGroup hiddenUserGroup : hiddenUserGroups) {
            if (hiddenUserGroup.getHiddenType() == 2 && this.canVisible(userId, hiddenUserGroup.getId())) {
                continue;
            }

            List<HiddenUser> hiddenUsers = this.listByHiddenUserGroupId(hiddenUserGroup.getId());
            hiddenUsers.forEach(u -> notIn.add(u.getUserId()));
        }

        return notIn;
    }

    private List<HiddenUserGroup> listByCorpId(Long corpId) {
        HiddenUserGroupExample example = new HiddenUserGroupExample();
        example.createCriteria().andCorpIdEqualTo(corpId);
        return hiddenUserGroupMapper.selectByExample(example);
    }


    public HiddenUserVisible getByUserId(Long userId, Long hiddenUserGroupId) {
        HiddenUserVisibleExample example = new HiddenUserVisibleExample();
        example.createCriteria().andUserIdEqualTo(userId).andHiddenUserGroupIdEqualTo(hiddenUserGroupId);
        return hiddenUserVisibleMapper.selectOne(example);
    }


    public boolean canVisible(Long userId, Long hiddenUserGroupId) {
        return getByUserId(userId, hiddenUserGroupId) != null;
    }

    public List<HiddenUser> listByHiddenUserGroupId(Long hiddenUserGroupId) {
        HiddenUserExample example = new HiddenUserExample();
        example.createCriteria().andHiddenUserGroupIdEqualTo(hiddenUserGroupId);
        return hiddenUserMapper.selectByExample(example);
    }


}
