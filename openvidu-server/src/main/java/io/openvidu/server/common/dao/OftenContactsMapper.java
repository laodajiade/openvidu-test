package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.OftenContacts;
import io.openvidu.server.common.pojo.vo.OftenContactsVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
public interface OftenContactsMapper {

    /**
     * 获取常用联系人列表
     *
     * @param map
     * @return
     */
    List<OftenContactsVo> getOftenContactsList(Map<String, Object> map);

    /**
     * 添加常用联系人
     *
     * @param oftenContacts
     * @return
     */
    int addOftenContacts(OftenContacts oftenContacts);


    /**
     * 删除常用联系人
     */
    int delOftenContacts(@Param("uuid") String uuid, @Param("userId") long userId);


    /**
     * 查看是否是常用联系人
     *
     * @param map
     * @return
     */
    boolean isOftenContacts(Map<String, Object> map);
}
