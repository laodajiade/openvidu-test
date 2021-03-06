package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Device;
import io.openvidu.server.common.pojo.DeviceSearch;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface DeviceMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Device record);

    int insertSelective(Device record);

    Device selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Device record);

    int updateBySerialNumberSelective(Device record);

    int updateByPrimaryKey(Device record);

    Device selectBySearchCondition(DeviceSearch search);

    Device selectBySerialNumber(@Param("serialNumber") String serialNumber);

    List<String> selectDevSerialNumsByDeptIds(List<Long> subDeptIds);

    List<Device> getDevicesBySerialNumsList(List<String> deviceSerialNumbers);

    int updateDeviceStatus(@Param("serialNumber") String serialNumber, @Param("status") String status);

    /**
     * 更新设备名称
     *
     * @param map
     * @return
     */
    int updateDeviceName(Map<String, Object> map);
}
