package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.HiddenPhoneMapper;
import io.openvidu.server.common.manage.HiddenPhoneManage;
import io.openvidu.server.common.pojo.AllUserInfo;
import io.openvidu.server.common.pojo.HiddenPhone;
import io.openvidu.server.common.pojo.HiddenPhoneExample;
import io.openvidu.server.common.pojo.UserGroupVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Service
public class HiddenPhoneManageImpl implements HiddenPhoneManage {


    @Autowired
    private HiddenPhoneMapper hiddenPhoneMapper;


    @Override
    public void hiddenPhone(List<UserGroupVo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        List<String> collect = list.stream().map(UserGroupVo::getUuid).collect(Collectors.toList());
        Set<String> needHiddleUuids = getNeedHiddenUuids(collect);

        if (needHiddleUuids.isEmpty()) {
            return;
        }
        for (UserGroupVo userGroupVo : list) {
            if (needHiddleUuids.contains(userGroupVo.getUuid())) {
                userGroupVo.setPhone("");
            }
        }
    }

    @Override
    public void hiddenPhone2(List<AllUserInfo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        List<String> collect = list.stream().map(AllUserInfo::getUuid).collect(Collectors.toList());
        Set<String> needHiddleUuids = getNeedHiddenUuids(collect);

        if (needHiddleUuids.isEmpty()) {
            return;
        }
        for (AllUserInfo userGroupVo : list) {
            if (needHiddleUuids.contains(userGroupVo.getUuid())) {
                userGroupVo.setPhone("");
            }
        }
    }

    private Set<String> getNeedHiddenUuids(List<String> uuids) {
        List<HiddenPhone> hiddenPhones = listByUuids(uuids);
        if (hiddenPhones.isEmpty()) {
            return Collections.emptySet();
        }
        return hiddenPhones.stream().map(HiddenPhone::getUuid).collect(Collectors.toSet());
    }


    private List<HiddenPhone> listByUuids(List<String> uuids) {
        HiddenPhoneExample example = new HiddenPhoneExample();
        example.createCriteria().andUuidIn(uuids);
        return hiddenPhoneMapper.selectByExample(example);
    }


    public boolean needHidden(String uuid) {
        return getNeedHiddenUuids(singletonList(uuid)).size() > 0;
    }
}
