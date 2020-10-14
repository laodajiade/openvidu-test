package io.openvidu.server.config;

import com.sensegigit.cockcrow.CrowOnceHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chosongi
 * @date 2020/3/19 22:08
 */
@Configuration
public class CockCrowConfig {
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.group.id}")
    private Long jobGroupId;

    @Bean
    public CrowOnceHelper crowOnceHelper() {
        CrowOnceHelper crowOnceHelper = new CrowOnceHelper();
        crowOnceHelper.setJobAdminAddr(adminAddresses);
        crowOnceHelper.setJobGroupId(jobGroupId);
        return crowOnceHelper;
    }
}
