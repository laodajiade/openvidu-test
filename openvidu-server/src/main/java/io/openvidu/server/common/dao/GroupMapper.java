package io.openvidu.server.common.dao;


import io.openvidu.server.common.pojo.Group;
//import io.openvidu.server.common.pojo.DepartmentTree;

import java.util.List;

public interface GroupMapper {

    int insert(Group record);

    int deleteByPrimaryKey(Long id);

    List<Group> selectByCorpIds(Long corpId);

    int updateByPrimaryKey(Group record);

    int deleteByCorpId(Long corpId);

    List<Group> getGroupByUserId(Long userId);



    int insertSelective(Group record);

    Group selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Group record);

//    List<DepartmentTree> selectByCorpId(Long corpId);
}