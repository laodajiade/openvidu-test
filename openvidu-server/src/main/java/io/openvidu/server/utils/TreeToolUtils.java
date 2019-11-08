package io.openvidu.server.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.openvidu.server.common.pojo.DepartmentTree;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author chosongi
 * @date 2019/10/18 22:32
 */
public class TreeToolUtils {
    private List<DepartmentTree> rootList;
    private List<DepartmentTree> bodyList;
    private List<Long> subDeptIds = Lists.newArrayList();

    public TreeToolUtils(List<DepartmentTree> rootList, List<DepartmentTree> bodyList) {
        this.rootList = rootList;
        this.bodyList = bodyList;
    }

    public List<DepartmentTree> getTree() {
        if (!CollectionUtils.isEmpty(bodyList)) {
            //声明一个map，用来过滤已操作过的数据
            Map<Long, Long> map = Maps.newHashMapWithExpectedSize(bodyList.size());
            rootList.forEach(beanTree -> getChild(beanTree, map));
            return rootList;
        }
        return null;
    }

    private void getChild(DepartmentTree beanTree, Map<Long, Long> map) {
        List<DepartmentTree> childList = Lists.newArrayList();
        bodyList.stream()
                .filter(c -> !map.containsKey(c.getOrgId()))
                .filter(c -> !Objects.isNull(c.getParentId()) &&
                        c.getParentId().compareTo(beanTree.getOrgId()) == 0)
                .forEach(c -> {
                    map.put(c.getOrgId(), c.getParentId());
                    getChild(c, map);
                    childList.add(c);

                    subDeptIds.add(c.getOrgId());
                });
        beanTree.setOrganizationList(childList);
    }

    public List<Long> getSubDeptIds() {
        return subDeptIds;
    }

}
