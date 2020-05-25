package io.openvidu.server.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class HttpUtil {

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final long TIME_OUT = 60;

    private OkHttpClient httpClient;

    @PostConstruct
    public void init() {
        httpClient = new OkHttpClient().newBuilder().readTimeout(TIME_OUT, TimeUnit.SECONDS)
                .connectTimeout(TIME_OUT, TimeUnit.SECONDS).build();
    }

    public JSONObject syncRequest(String url, String requestBody) {
        try {
            Request request = constructRequest(url, requestBody);
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                assert response.body() != null;
                String respResult = response.body().string();
                log.info("Request succeeded. Uri:{}, response:{}", request.url(), respResult);
                return JSON.parseObject(respResult);
            } else {
                log.info("Sync request failed. Uri:{}, Request body:{}", url, requestBody);
            }
        } catch (Exception e) {
            log.info("Sync request failed. Uri:{}, Exception:{}", url, e);
        }
        return null;
    }

    public JSONObject asyncRequest(String uri, String requestBody) {
        try {
            httpClient.newCall(constructRequest(uri, requestBody)).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        System.out.println(JSON.parseObject(response.body().string()));
                        log.info("Async request succeed. Uri:{}, Request body:{}. \n{}", uri, requestBody);
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    log.info("Async request failed. Uri:{}, Request body:{}. \n{}", uri, requestBody, e);
                }
            });
        } catch (Exception e) {
            log.info("Exception:", e);
        }
        return null;
    }

    private static Request constructRequest(String uri, String requestBody) {
        RequestBody body = RequestBody.create(MEDIA_TYPE, requestBody.getBytes(Charset.forName("UTF-8")));
        return new Request.Builder()
                .url(uri)
                .post(body)
                .addHeader("Content-Type", org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .addHeader("Content-Length", String.valueOf(requestBody.length()))
                .build();
    }

}

