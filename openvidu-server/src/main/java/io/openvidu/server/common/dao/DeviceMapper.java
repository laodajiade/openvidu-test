package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeviceMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Device record);

    int insertSelective(Device record);

    Device selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Device record);

    int updateBySerialNumberSelective(Device record);

    int updateByPrimaryKey(Device record);

    Device selectBySearchCondition(DeviceSearch search);

    List<String> selectDevSerialNumsByDeptIds(List<Long> subDeptIds);

    List<Device> getDevicesBySerialNumsList(List<String> deviceSerialNumbers);

    int updateDeviceStatus(@Param("serialNumber") String serialNumber, @Param("status") String status);
}
