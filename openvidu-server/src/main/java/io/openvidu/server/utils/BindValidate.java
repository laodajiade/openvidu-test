package io.openvidu.server.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.openvidu.server.exception.BindValidateException;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.NotSupportedException;
import java.util.Collection;
import java.util.function.Supplier;
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

    public static void notEmpty(BindSupplier<?>... suppliers) {
        if (suppliers == null) {
            throw new BindValidateException(" 参数错误");
        }
        for (BindSupplier<?> supplier : suppliers) {
            notEmpty(supplier);
        }
    }

    public static void notEmpty(JsonObject param, String jsonPath) {
        if (!param.has(jsonPath)) {
            throw new BindValidateException(jsonPath + " 不能为 empty");
        }
        if (StringUtils.isEmpty(param.get(jsonPath).getAsString())) {
            throw new BindValidateException(jsonPath + " 不能为 empty");
        }
    }

    public static String notEmptyAndGet(JsonObject param, String jsonPath) {
        if (!param.has(jsonPath)) {
            throw new BindValidateException(jsonPath + " 不能为 empty");
        }
        if (StringUtils.isEmpty(param.get(jsonPath).getAsString())) {
            throw new BindValidateException(jsonPath + " 不能为 empty");
        }
        return param.get(jsonPath).getAsString();
    }

    @SuppressWarnings("unchecked")
    public static <T> T notEmptyAndGet(JsonObject param, String jsonPath, Class<T> clazz) {
        if (!param.has(jsonPath)) {
            throw new BindValidateException(jsonPath + " 不能为 empty");
        }
        if (clazz == String.class) {
            if (StringUtils.isEmpty(param.get(jsonPath).getAsString())) {
                throw new BindValidateException(jsonPath + " 不能为 empty");
            }
            return (T) param.get(jsonPath).getAsString();
        } else if (clazz == Integer.class) {
            return (T) Integer.valueOf(param.get(jsonPath).getAsString());
        } else if (clazz == Long.class) {
            return (T) Long.valueOf(param.get(jsonPath).getAsLong());
        } else if (clazz == Float.class) {
            return (T) Float.valueOf(param.get(jsonPath).getAsString());
        } else if (clazz == Double.class) {
            return (T) Double.valueOf(param.get(jsonPath).getAsString());
        } else if (clazz.isEnum()) {
            T[] enumConstants = clazz.getEnumConstants();
            for (T enumConstant : enumConstants) {
                if (((Enum) enumConstant).name().equals(param.get(jsonPath).getAsString())) {
                    return enumConstant;
                }
            }
            throw new BindValidateException(jsonPath + " 枚举类型错误");
        } else if (clazz == JsonArray.class) {
            return (T) param.getAsJsonArray(jsonPath);
        }
        throw new NotSupportedException("类型不支持，还在施工");
    }

    /**
     * 如果matchCondition的条件为true，则校验supplier
     *
     * @param matchCondition 前置条件
     * @param supplier       校验条件
     */
    //todo yy 如果有时间可以试试用 Predicate代替 Supplier<Boolean>
    public static void notEmptyIfMatch(Supplier<Boolean> matchCondition, BindSupplier<?> supplier) {
        if (matchCondition == null || !matchCondition.get()) {
            return;
        }
        notEmpty(supplier);
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
