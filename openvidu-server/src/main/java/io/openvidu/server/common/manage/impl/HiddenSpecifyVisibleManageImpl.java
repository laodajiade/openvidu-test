package io.openvidu.server.common.manage.impl;

import io.openvidu.server.common.dao.HiddenSpecifyVisibleMapper;
import io.openvidu.server.common.dao.UserDeptMapper;
import io.openvidu.server.common.manage.DepartmentManage;
import io.openvidu.server.common.manage.HiddenSpecifyVisibleManage;
import io.openvidu.server.common.pojo.HiddenSpecifyVisible;
import io.openvidu.server.common.pojo.HiddenSpecifyVisibleExample;
import io.openvidu.server.common.pojo.UserDept;
import io.openvidu.server.common.pojo.dto.HiddenSpecifyVisibleDTO;
import io.openvidu.server.common.pojo.dto.SpecifyVisibleRule;
import io.openvidu.server.service.UserDeptService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Service
public class HiddenSpecifyVisibleManageImpl implements HiddenSpecifyVisibleManage {


    @Autowired
    HiddenSpecifyVisibleMapper hiddenSpecifyVisibleMapper;

    @Autowired
    private DepartmentManage departmentManage;

    @Autowired
    private UserDeptService userDeptService;

    @Resource
    private UserDeptMapper userDeptMapper;

    public HiddenSpecifyVisibleDTO getSpecifyVisibleRule(String uuid, Long userId, Long corpId) {
        HiddenSpecifyVisibleExample example = new HiddenSpecifyVisibleExample();
        example.createCriteria().andCorpIdEqualTo(corpId).andLimitUserLike("%" + uuid + "%");
        List<HiddenSpecifyVisible> hiddenSpecifyVisibles = hiddenSpecifyVisibleMapper.selectByExample(example);

        if (hiddenSpecifyVisibles.isEmpty()) {
            return new HiddenSpecifyVisibleDTO(2);
        }

        Optional<HiddenSpecifyVisible> find = hiddenSpecifyVisibles.stream().filter(h -> h.getType() == 0).findFirst();
        if (find.isPresent()) {
            return new HiddenSpecifyVisibleDTO(0);
        }
        HiddenSpecifyVisibleDTO dto = new HiddenSpecifyVisibleDTO(1);
        dto.setUserId(userId);
        for (HiddenSpecifyVisible hiddenSpecifyVisible : hiddenSpecifyVisibles) {
            if (StringUtils.isNotBlank(hiddenSpecifyVisible.getVisibleUser())) {
                dto.getVisibleUser().addAll(Arrays.stream(hiddenSpecifyVisible.getVisibleUser().split(",")).map(Long::new).collect(Collectors.toSet()));
            }

            dto.setDeptVisible(Math.max(dto.getDeptVisible(), hiddenSpecifyVisible.getDeptVisible()));
        }

        if (dto.getDeptVisible() == 1) {
            UserDept userDept = userDeptService.getByUserId(dto.getUserId());
            List<Long> allChildDept = departmentManage.getAllChildDept(singletonList(userDept.getDeptId()));
            dto.setVisibleDept(new HashSet<>(allChildDept));
        }

        if (!dto.getVisibleDept().isEmpty()) {
            List<UserDept> uds = userDeptMapper.selectByDeptIdsList(new ArrayList<>(dto.getVisibleDept()));
            Set<Long> collect = uds.stream().map(UserDept::getUserId).collect(Collectors.toSet());
            dto.getVisibleUser().addAll(collect);
        }

        return dto;
    }


    public SpecifyVisibleRule getSpecifyVisibleRule2(String uuid, Long userId, Long corpId) {
        HiddenSpecifyVisibleExample example = new HiddenSpecifyVisibleExample();
        example.createCriteria().andCorpIdEqualTo(corpId).andLimitUserLike("%" + uuid + "%");
        List<HiddenSpecifyVisible> hiddenSpecifyVisibles = hiddenSpecifyVisibleMapper.selectByExample(example);


        if (hiddenSpecifyVisibles.isEmpty()) {
            return new SpecifyVisibleRule(2);
        }

        Optional<HiddenSpecifyVisible> find = hiddenSpecifyVisibles.stream().filter(h -> h.getType() == 0).findFirst();
        if (find.isPresent()) {
            return new SpecifyVisibleRule(0);
        }
        SpecifyVisibleRule dto = new SpecifyVisibleRule(1);

        int deptVisible = 0;
        for (HiddenSpecifyVisible hiddenSpecifyVisible : hiddenSpecifyVisibles) {
            if (StringUtils.isNotBlank(hiddenSpecifyVisible.getVisibleUser())) {
                dto.getVisibleUser().addAll(Arrays.stream(hiddenSpecifyVisible.getVisibleUser().split(",")).map(Long::new).collect(Collectors.toSet()));
            }

            deptVisible = Math.max(deptVisible, hiddenSpecifyVisible.getDeptVisible());
        }

        if (deptVisible == 1) {
            UserDept userDept = userDeptService.getByUserId(userId);
            List<Long> allChildDept = departmentManage.getAllChildDept(singletonList(userDept.getDeptId()));

            List<UserDept> userDepts = userDeptService.getByDeptIds(allChildDept);

            for (UserDept ud : userDepts) {
                dto.getVisibleUser().add(ud.getUserId());
            }
        }

        return dto;
    }
}
