package io.openvidu.server.job;

import com.alibaba.fastjson.JSONObject;
import io.openvidu.server.common.cache.CacheManage;
import io.openvidu.server.common.pojo.DongleInfo;
import io.openvidu.server.utils.DESUtil;
import io.openvidu.server.utils.SecurityResposne;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @program: prepaid-platform
 * @description: 启动加载类
 * @author: WuBing
 * @create: 2021-06-23 15:35
 **/
@Component
@Slf4j
@Order(value = 1)
public class StartLoader {

    @Autowired
    CacheManage cacheManage;

    @Autowired
    DognelScheduledExecutor dognelScheduledExecutor;

    @Value("${dongle.minRunTime}")
    public Integer minRunTime;

    @Value("${dongle.maxRunTime}")
    public Integer maxRunTime;

    @Value("${rhkey}")
    public String rhkey;

    @Value("${rlkey}")
    public String rlkey;

    @Value("${publicKey}")
    public String publicKey;

    @Autowired
    ConfigurableApplicationContext ctx;


    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

    public void check() {
        log.info("++++++++++++++++++++++++++++++++++++++++++++++++start Dongle++++++++++++++++++++++++++++++++++++++++++");
        checkDognle();
        checkDognelSchedule();
    }


    public void checkDognle() {
        try {
            Class<?> cls = Class.forName("io.openvidu.server.utils.CheckUtils");
            cls.newInstance();
            Method check = cls.getMethod("checkDog", String.class, String.class, String.class);
            SecurityResposne securityResposne = (SecurityResposne) check.invoke(cls.newInstance(), rhkey, rlkey, publicKey);
            if (securityResposne.getCode() != 200) {
                log.error("checkDognleError  ERROR CODE:{}；{}", securityResposne.getCode(), securityResposne.getMsg());
                int exit = SpringApplication.exit(ctx, () -> 0);
                System.exit(exit);
            }
            DongleInfo dongleInfo = (DongleInfo) securityResposne.getData();
            DognelScheduledExecutor.dongleInfo = dongleInfo;
            String dongle = JSONObject.toJSONString(dongleInfo);
            String dongleinfo = DESUtil.encrypt(dongle);
            cacheManage.setCropDongleInfo(dongleinfo);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("start checkDongleError,启动失败,{}", e.getMessage());
            int exit = SpringApplication.exit(ctx, () -> 0);
            System.exit(exit);
        }
    }

    public void checkDognelSchedule() {
        service.scheduleWithFixedDelay(dognelScheduledExecutor, 0, minRunTime * 60, TimeUnit.SECONDS);
    }


}
