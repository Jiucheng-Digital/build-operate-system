package com.neucity.common.security.utils;

import com.neucity.common.core.model.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/9/2 11:08
 */
@Slf4j
public class WebUserUtil {
    /**
     * 新增时添加用户以及当前时间信息
     */
    public static <T> void prepareInsertParams(T obj) {
        LoginUser authUser = SecurityUtils.getLoginUser();
        if (authUser != null && authUser.getUsername() != null) {
            // 创建人
//            invokeSet(obj, "userId", SecurityUtils.getUserId(), false);
            // 创建人名称
            invokeSet(obj, "createBy", authUser.getUsername(), false);
            // 更新人名称
            invokeSet(obj, "updateBy", authUser.getUsername(), false);
        }

        Date date = new Date();
        // 更新时间
        invokeSet(obj, "createTime", date, false);
        // 更新时间
        invokeSet(obj, "updateTime", date, false);
        // 初始版本号
        invokeSet(obj, "version", 1L, false);
    }


    /**
     * 更新时添加用户以及当前时间信息
     */
    public static <T> void prepareUpdateParams(T obj) {
        LoginUser authUser = SecurityUtils.getLoginUser();
        if (authUser != null && authUser.getUsername() != null) {
            // 更新人
            invokeSet(obj, "updatedBy", authUser.getUsername(), true);
        } else {
            invokeSet(obj, "updatedBy", "sys", true);
        }
        // 更新时间
        Date date = new Date();
        invokeSet(obj, "updateTime", date,  true);
    }

    /**
     * 执行set方法
     *
     * @param o         执行对象
     * @param fieldName 属性
     * @param value     值
     */
    private static void invokeSet(Object o, String fieldName, Object value, boolean cover) {
        try {
            Field field = getField(org.springframework.util.ClassUtils.getUserClass(o), fieldName);
            field.setAccessible(true);
            Object nameValue = field.get(o);
            if (!cover && nameValue != null) {
                return;
            }

            Method method = getSetMethod(org.springframework.util.ClassUtils.getUserClass(o), fieldName);
            method.invoke(o, value);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * java反射bean的set方法
     */
    @SuppressWarnings("rawtypes")
    private static Method getSetMethod(Class<? extends Object> objectClass, String fieldName) {

        try {
            Class[] parameterTypes = new Class[1];
            Field field = getField(objectClass, fieldName);
            parameterTypes[0] = field.getType();
            StringBuffer sb = new StringBuffer();
            sb.append("set");
            sb.append(fieldName.substring(0, 1).toUpperCase());
            sb.append(fieldName.substring(1));
            return objectClass.getMethod(sb.toString(), parameterTypes);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;

    }

    /**
     * 获取类已经类的父的某一声明变量
     *
     * @param fieldName 变量名
     * @return Field 变量
     */
    @SuppressWarnings("rawtypes")
    private static Field getField(Class clazz, String fieldName) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            List<Class<?>> allSuperclasses = ClassUtils.getAllSuperclasses(clazz);
            Class superClass = null;
            if (!CollectionUtils.isEmpty(allSuperclasses)) {
                superClass = allSuperclasses.get(0);
            }
            if (superClass == null) {
                throw e;
            } else {
                return getField(superClass, fieldName);
            }
        }
    }

}
