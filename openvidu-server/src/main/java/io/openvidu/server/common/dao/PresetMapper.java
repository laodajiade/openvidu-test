package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.Preset;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author even
 * @date 2020/8/28 10:42
 */
public interface PresetMapper {

    /**
     * 新增预置点
     * @param preset 预置点信息
     * @return int
     */
    int insert(Preset preset);

    /**
     * 更新预置点
     * @param preset 预置点信息
     * @return int
     */
    int update(Preset preset);

    /**
     * 删除预置点
     * @param serialNumber 设备序列号
     * @param list 索引集合
     * @return int
     */
    int deleteByIndexs(@Param("serialNumber") String serialNumber, @Param("list") List<Long> list);

    /**
     * 通过设备序列号、索引获取预置点信息
     * @param serialNumber 设备序列号
     * @param index 索引
     * @return Preset
     */
    Preset selectBySerialNumberAndIndex(@Param("serialNumber") String serialNumber, @Param("index") Integer index);

    /**
     * 获取预置点列表
     * @param serialNumber 设备序列号
     * @return List<Preset>
     */
    List<Preset> selectList(String serialNumber);
}
