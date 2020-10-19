package io.openvidu.server.utils;

import com.google.gson.JsonObject;
import io.openvidu.server.exception.BindValidateException;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.regex.Pattern;

public class BindValidate {

    public static void notNull(BindSupplier<?> supplier) {
        if (supplier == null) {
            throw new BindValidateException(" 参数错误");
        }
        if (supplier.get() == null) {
            throwBindValidateException(supplier, " 不能为 null");
        }
    }

    public static void notEmpty(BindSupplier<?> supplier) {
        if (supplier == null) {
            throw new BindValidateException(" 参数错误");
        }
        Object obj = supplier.get();
        if (obj == null) {
            throwBindValidateException(supplier, " 不能为 empty");
        }

        if (obj instanceof String) {
            if (StringUtils.isEmpty((String) obj)) {
                throwBindValidateException(supplier, " 不能为 empty");
            }
        } else if (obj instanceof Collection) {
            if (((Collection) obj).isEmpty()) {
                throwBindValidateException(supplier, " 不能为 empty");
            }
        }
    }


    public static void notEmpty(JsonObject param, String jsonPath) {
        if (!param.has(jsonPath)) {
            throw new BindValidateException(jsonPath + " 不能为 empty");
        }
        if (org.apache.commons.lang.StringUtils.isEmpty(param.get(jsonPath).getAsString())) {
            throw new BindValidateException(jsonPath + " 不能为 empty");
        }
    }

    private static void throwBindValidateException(BindSupplier<?> supplier, String s) {
        String method = supplier.getImplMethodName();
        if (StringUtils.startsWith(method, "get")) {
            method = method.substring(3);
            method = Character.toLowerCase(method.charAt(0)) + method.substring(1);
        }
        throw new BindValidateException(method + s);
    }

    public static void isPhoneNumber(BindSupplier<String> supplier) {
        notNull(supplier);

        if (!Pattern.compile("1\\d{10}").matcher(supplier.get()).matches()) {
            throw new BindValidateException(supplier.get() + " 手机号格式不对");
        }
    }
}
