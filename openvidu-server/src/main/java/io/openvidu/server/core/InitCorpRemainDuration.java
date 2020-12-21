package io.openvidu.server.core;

import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.pojo.Corporation;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * @author even
 * @date 2020/12/21 13:58
 */
@Component
public class InitCorpRemainDuration {

    @Resource
    private CorporationMapper corporationMapper;

    @Resource
    CacheManage cacheManage;

    @PostConstruct
    public void initRemainDuration() {
        List<Corporation> corporations = corporationMapper.selectAllCorp();
        if (!CollectionUtils.isEmpty(corporations)) {
            corporations.forEach(corporation -> {
                cacheManage.setCorpRemainDuration(corporation.getProject(), corporation.getRemainderDuration());
            });
        }
    }
}
