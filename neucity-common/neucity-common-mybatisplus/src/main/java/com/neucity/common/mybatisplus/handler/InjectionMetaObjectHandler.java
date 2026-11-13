//package com.cqxf.common.mybatisplus.handler;
//
//import cn.hutool.core.util.ObjectUtil;
//import cn.hutool.http.HttpStatus;
//import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
//import com.cqxf.common.core.constant.SecurityConstants;
//import com.cqxf.common.core.context.SecurityContextHolder;
//import com.cqxf.common.core.domain.SysUser;
//import com.cqxf.common.core.model.LoginUser;
//import com.cqxf.common.core.web.domain.BaseEntity;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.ibatis.reflection.MetaObject;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//
///**
// * MP注入处理器
// * @author hcd
// */
//@Component
//@Slf4j
//public class InjectionMetaObjectHandler implements MetaObjectHandler {
//
//    /**
//     * 插入填充方法，用于在插入数据时自动填充实体对象中的创建时间、更新时间、创建人、更新人等信息
//     *
//     * @param metaObject 元对象，用于获取原始对象并进行填充
//     */
//    @Override
//    public void insertFill(MetaObject metaObject) {
//        try {
//            if (ObjectUtil.isNotNull(metaObject) && metaObject.getOriginalObject() instanceof BaseEntity) {
//                BaseEntity baseEntity = (BaseEntity) metaObject.getOriginalObject();
//                Date current = new Date();
//                baseEntity.setCreateTime(current);
//                baseEntity.setUpdateTime(current);
//
//                SysUser loginUser = getLoginUser();
//                if (ObjectUtil.isNotNull(loginUser)) {
//                    baseEntity.setCreateBy(loginUser.getUserName());
//                    baseEntity.setUpdateBy(loginUser.getUserName());
//                }
//            }
//        } catch (Exception e) {
//           log.error("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
//        }
//    }
//
//    /**
//     * 更新填充方法，用于在更新数据时自动填充实体对象中的更新时间和更新人信息
//     *
//     * @param metaObject 元对象，用于获取原始对象并进行填充
//     */
//    @Override
//    public void updateFill(MetaObject metaObject) {
//        try {
//            if (ObjectUtil.isNotNull(metaObject) && metaObject.getOriginalObject() instanceof BaseEntity) {
//                BaseEntity baseEntity = (BaseEntity) metaObject.getOriginalObject();
//
//                // 1. 处理updateTime
//                if (metaObject.hasSetter("updateTime")) {
//                    // 检查是否已经设置了updateTime值
//                    Object updateTimeValue = metaObject.getValue("updateTime");
//                    if (updateTimeValue == null) {
//                        // 如果没有传入值，则自动填充当前时间
//                        baseEntity.setUpdateTime(new Date());
//                    }
//                }
//
//
//
//                SysUser loginUser = getLoginUser();
//                if (ObjectUtil.isNotNull(loginUser)) {
//                    baseEntity.setUpdateBy(loginUser.getUserName());
//                }
//            }
//        } catch (Exception e) {
//            log.error("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
//        }
//    }
//
//    /**
//     * 获取当前登录用户信息
//     *
//     * @return 当前登录用户的信息，如果用户未登录则返回 null
//     */
//    private SysUser getLoginUser() {
//        SysUser loginUser;
//        try {
//            loginUser = SecurityContextHolder.get(SecurityConstants.LOGIN_USER, LoginUser.class).getSysUser();
//        } catch (Exception e) {
//            return null;
//        }
//
//        return loginUser;
//    }
//
//}
