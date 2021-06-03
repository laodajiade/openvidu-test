package io.openvidu.server.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Administrator
 */
@Slf4j
public class ReflexUtils {


    /**
     * 获取属性名数组
     */
    public static String[] getFieldName(Object o) {
        Field[] fields = o.getClass().getDeclaredFields();
        String[] fieldNames = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldNames[i] = fields[i].getName();
        }
        return fieldNames;
    }

    /**
     * 通过属性名获取属性值  忽略大小写
     *
     * @param o
     * @param name
     * @return
     * @throws Exception
     */

    public static Object getFieldValue(Object o, String name) {
        Field[] fields = o.getClass().getDeclaredFields();
        Object object = null;
        try {
            for (Field field : fields) {
                field.setAccessible(true);//可以获取到私有属性
                if (field.getName().toUpperCase().equals(name.toUpperCase())) {
                    object = field.get(o);
                    break;
                }
            }
        } catch (Exception e) {
            log.info("获取属性失败{}", e.getMessage());
        }
        return object;
    }

    /**
     * 对象属性赋值
     *
     * @param o
     * @param name
     * @param value
     */
    public static void setFieldValue(Object o, String name, String value) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(o, value);
        } catch (Exception e) {
            log.info("赋值失败{}", e.getMessage());
        }
    }
}
