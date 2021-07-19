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
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @program: prepaid-platform
 * @description: 加密定时器
 * @author: WuBing
 * @create: 2021-06-24 09:58
 **/
@Slf4j
@Component
public class DognelScheduledExecutor implements Runnable {


    @Autowired
    CacheManage cacheManage;


    @Value("${dongle.minRunTime:5}")
    public Integer minRunTime;

    @Value("${dongle.maxRunTime:10}")
    public Integer maxRunTime;

    @Value("${rhkey}")
    public String rhkey;

    @Value("${rlkey}")
    public String rlkey;

    @Value("${publicKey}")
    public String publicKey;

    @Autowired
    ConfigurableApplicationContext ctx;
    public static DongleInfo dongleInfo = null;


    @Override
    public void run() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = sdf.format(new Date());
        Class<?> cls = null;
        try {
            Thread.sleep(getNextRunCron(minRunTime, maxRunTime) * 1000);
            cls = Class.forName("io.openvidu.server.utils.CheckUtils");
            Method check = cls.getMethod("checkDog", String.class, String.class, String.class);
            SecurityResposne securityResposne = (SecurityResposne) check.invoke(cls.newInstance(), rhkey, rlkey, publicKey);
            if (securityResposne.getCode() != 200) {
                log.error("check Dongle error  ERROR CODE:{}；{}", securityResposne.getCode(), securityResposne.getMsg());
                int exit = SpringApplication.exit(ctx, () -> 0);
                System.exit(exit);
            }
            dongleInfo = (DongleInfo) securityResposne.getData();
            String dongle = JSONObject.toJSONString(dongleInfo);
            String encrypt = DESUtil.encrypt(dongle);
            cacheManage.setCropDongleInfo(encrypt);
            log.info("check Dongle time:{}", format);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("加密狗验证失败,退出程序;{}", e.getMessage());
            int exit = SpringApplication.exit(ctx, () -> 0);
            System.exit(exit);
        }
    }


    public int getNextRunCron(Integer minRunTime, Integer maxRunTime) {
        //默认5-10分钟
        if (maxRunTime > 0 && maxRunTime > minRunTime) {
            minRunTime = minRunTime * 60;
            maxRunTime = maxRunTime * 60;
        } else {
            minRunTime = 5 * 60;
            maxRunTime = 10 * 60;
        }
        int randoom = (int) (Math.random() * (maxRunTime - minRunTime) + minRunTime);
        return randoom;
    }

}
