package io.openvidu.server.common.dao;

import io.openvidu.server.common.pojo.OftenContacts;
import io.openvidu.server.common.pojo.vo.OftenContactsVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Administrator
 */
public interface OftenContactsMapper {

    /**
     * 获取常用联系人列表
     *
     * @param userId
     * @param userIds
     * @return
     */
    List<OftenContactsVo> getOftenContactsList(@Param("userId") Long userId,@Param("list") Set<Long> list);

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
