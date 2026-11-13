package com.neucity.common.mybatisplus.util;

import cn.hutool.core.util.IdUtil;

import java.util.UUID;

/**
 * id生成工具类
 *
 * @author yoga
 */
public class IdWorker {

    /**
     * 生成id
     */
    public static long generateId() {
        // todo 集群模式下增加机器码
        return IdUtil.getSnowflake().nextId();
    }

    /**
     * 生成uuid
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
