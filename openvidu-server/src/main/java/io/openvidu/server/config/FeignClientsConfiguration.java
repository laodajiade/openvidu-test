package io.openvidu.server.config;

import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * feign配置，带上header
 *
 * @author wangliang
 */
@Configuration
@EnableFeignClients("io.openvidu.server")
@Slf4j
public class FeignClientsConfiguration implements RequestInterceptor {

    @Value("${spring.profiles.active:test}")
    private String env;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        long start = System.currentTimeMillis();
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes == null) {
                return;
            }

            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    Enumeration<String> values = request.getHeaders(name);
                    while (values.hasMoreElements()) {
                        String value = values.nextElement();
                        requestTemplate.header(name, value);
                    }
                }
            }
        } finally {
            long end = System.currentTimeMillis();
            if (end - start >= 5000) {
                log.info(requestTemplate.url() + " 超时： " + (end - start) + "ms");
            }
        }
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        if ("dev".equals(env)){
            return Logger.Level.NONE;
        }
        //这里记录所有，根据实际情况选择合适的日志level
        return Logger.Level.NONE;
    }
}
