package io.openvidu.server.rpc;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.openvidu.server.common.enums.ErrorCodeEnum;
import io.openvidu.server.core.Notification;
import io.openvidu.server.core.RespResult;
import io.openvidu.server.exception.BindValidateException;
import io.openvidu.server.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.kurento.jsonrpc.message.Request;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author yy
 */
@Slf4j
@Component
public abstract class ExRpcAbstractHandler<P> extends RpcAbstractHandler {

    //Gson gson = new GsonBuilder().setPrettyPrinting().create();
    Gson gson = new GsonBuilder().create();

    @Override
    public void handRpcRequest(RpcConnection rpcConnection, Request<JsonObject> request) {
        try {
            Request<P> genRequest = convertParams(request);
            log.info("ex request:{}", genRequest);
            RespResult<?> respResult = doProcess(rpcConnection, genRequest, genRequest.getParams());
            log.info("ex response:{}", gson.toJson(respResult));
            if (!respResult.isOk()) {
                notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                        respResult.getResult(), respResult.getCode());
                return;
            }


            Object result = respResult.getResult();
            if (result == null) {
                result = new JsonObject();
            }
            notificationService.sendResponse(rpcConnection.getParticipantPrivateId(), request.getId(), result);
            Notification notification = respResult.getNotification();
            if (notification != null) {
                notificationService.sendBatchNotification(notification.getParticipantIds(), notification.getMethod(), notification.getParams());
            }
        } catch (BizException e) {
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, e.getRespEnum());
        } catch (BindValidateException e) {
            log.info("BindValidateException " + e.getMessage());
            notificationService.sendErrorResponseWithDesc(rpcConnection.getParticipantPrivateId(), request.getId(),
                    null, ErrorCodeEnum.REQUEST_PARAMS_ERROR);
        }

    }

    public abstract RespResult<?> doProcess(RpcConnection rpcConnection, Request<P> request, P params);


    private Request<P> convertParams(Request<JsonObject> request) {
        Class<P> paramsType = getParamsType();
        if ("com.google.gson.JsonObject".equals(paramsType.getName())) {
            return new Request<>(request.getSessionId(), request.getId(), request.getMethod(), (P) request.getParams());
        }
        String json = request.getParams().toString();
        P param = JSON.parseObject(json, paramsType);
        return new Request<>(request.getSessionId(), request.getId(), request.getMethod(), param);
    }

    private final static int DEEP = 3;

    @SuppressWarnings("unchecked")
    public Class<P> getParamsType() {
        Type genType = this.getClass().getGenericSuperclass();

        int tryCnt = 0;
        while (!(genType instanceof ParameterizedType) && tryCnt++ < DEEP) {
            genType = ((Class) genType).getGenericSuperclass();
        }
        if (!(genType instanceof ParameterizedType)) {
            return (Class<P>) genType.getClass();
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        return (Class<P>) params[0];
    }
}
