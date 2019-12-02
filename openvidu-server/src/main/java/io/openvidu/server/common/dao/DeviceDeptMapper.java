package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.DeviceDept;
import io.openvidu.server.common.pojo.DeviceDeptSearch;

import java.util.List;

public interface DeviceDeptMapper {

    int deleteByPrimaryKey(Long id);

    int insert(DeviceDept record);

    int insertSelective(DeviceDept record);

    DeviceDept selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DeviceDept record);

    int updateByPrimaryKey(DeviceDept record);

    List<DeviceDept> selectBySearchCondition(DeviceDeptSearch search);

    List<DeviceDept> selectByCorpId(Long corp);

    Long selectCorpByOrgId(Long orgId);
}