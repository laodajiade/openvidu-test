package io.openvidu.server.job;

import io.openvidu.server.common.broker.CorpServiceExpiredNotifyHandler;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.dao.CorporationMapper;
import io.openvidu.server.common.pojo.Corporation;
import io.openvidu.server.rpc.RpcNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class CorpExpireSchedule {

    @Resource
    private CorporationMapper corporationMapper;

    @Autowired
    protected RpcNotificationService notificationService;
    @Resource
    protected CacheManage cacheManage;

    @Autowired
    protected CorpServiceExpiredNotifyHandler corpServiceExpiredNotifyHandler;

    /**
     * 每天凌晨0点通知服务已到期
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void corpExpiredNotify() {
        List<Corporation> corporations = corporationMapper.listCorpExpire();
        corporations.forEach(corp -> corpServiceExpiredNotifyHandler.notify(corp.getId().toString()));
    }


}
