package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.OftenContacts;
import io.openvidu.server.common.pojo.vo.OftenContactsVo;

import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 */
public interface OftenContactsMapper {

    /**
     * 获取常用联系人列表
     *
     * @param userId
     * @return
     */
    List<OftenContactsVo> getOftenContactsList(Long userId);

    /**
     * 添加常用联系人
     *
     * @param oftenContacts
     * @return
     */
    int addOftenContacts(OftenContacts oftenContacts);


    /**
     * 删除常用联系人
     *
     * @param uuid
     * @return
     */
    int delOftenContacts(String uuid);


    /**
     * 查看是否是常用联系人
     * @param map
     * @return
     */
    boolean  isOftenContacts(Map<String,Object> map);
}
