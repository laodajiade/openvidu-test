package io.openvidu.server.utils;

import org.springframework.cglib.beans.BeanCopier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BeanUtils {


    /**
     * 这个方法效率很低,不建议使用
     *
     * @param source
     * @param targetClazz
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> T copyProperties(Object source, Class targetClazz) {
        if (source == null) {
            return null;
        }
        try {
            if (source instanceof Collection) {
                List list = new ArrayList();
                for (Object obj : (Collection) source) {
                    Object target = targetClazz.newInstance();
                    org.springframework.beans.BeanUtils.copyProperties(obj, target);
                    list.add(target);
                }
                return (T) list;
            } else {
                Object target = targetClazz.newInstance();
                org.springframework.beans.BeanUtils.copyProperties(source, target);
                return (T) target;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对象拷贝
     * 注意：这里只会拷贝类型和属性名相同的属性,即使属性名相同 但是类型为原始类型和包装类型也不会拷贝
     *
     * @param source
     * @param target
     */
    public static void beanToBean(Object source, Object target) {
        BeanCopier copier = BeanCopier.create(source.getClass(), target.getClass(), false);
        copier.copy(source, target, null);
    }

    /**
     * 对象拷贝
     * 注意：这里只会拷贝类型和属性名相同的属性,即使属性名相同 但是类型为原始类型和包装类型也不会拷贝
     *
     * @param source
     * @param target
     */
    public static <T> T copyToBean(Object source, Class<T> target) {
        try {
            Object object = target.newInstance();
            beanToBean(source, object);
            return (T) object;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 数组拷贝
     *
     * @param source 源数组
     * @param target 要复制的源对象类
     * @return
     */
    public static <T, K> List<T> listToList(List<K> source, Class<T> target) {
        try {
            List<T> list = new ArrayList<>(source.size());
            for (Object item : source) {
                Object object = target.newInstance();
                beanToBean(item, object);
                list.add((T) object);
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
