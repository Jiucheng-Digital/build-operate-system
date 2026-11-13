package com.neucity.common.redis.constants;

/**
 * Redis Lua 脚本
 *
 * @author ShuPF
 * @date 2025/10/7 10:11
 */
public class RedisLuaConstant {

    /**
     *  令牌桶限流脚本
     */
    public static final String LPT_RATE_LIMIT_SCRIPT = "local key = KEYS[1]\n" +
            "\n" +
            "-- 安全的数字转换函数\n" +
            "local function safe_tonumber(str)\n" +
            "    if str == nil then\n" +
            "        return nil\n" +
            "    end\n" +
            "    if type(str) == \"number\" then\n" +
            "        return str\n" +
            "    end\n" +
            "    if type(str) == \"string\" then\n" +
            "        -- 清理字符串：移除空格和特殊字符\n" +
            "        local cleaned = string.gsub(str, \"%s+\", \"\")\n" +
            "        cleaned = string.gsub(cleaned, \"[^%d%.]\", \"\")\n" +
            "        return tonumber(cleaned)\n" +
            "    end\n" +
            "    return nil\n" +
            "end\n" +
            "\n" +
            "local qps = safe_tonumber(ARGV[1])\n" +
            "local timeWindow = safe_tonumber(ARGV[2])\n" +
            "local now = safe_tonumber(ARGV[3])\n" +
            "local waitable = ARGV[4]\n" +
            "local timeout = safe_tonumber(ARGV[5])\n" +
            "\n" +
            "-- 记录所有参数的值和类型\n" +
            "redis.log(redis.LOG_NOTICE, \"====== Rate Limit Script Parameters ======\")\n" +
            "redis.log(redis.LOG_NOTICE, \"KEY[1]: \" .. tostring(key))\n" +
            "redis.log(redis.LOG_NOTICE, \"ARGV[1] (qps): \" .. tostring(ARGV[1]) .. \", as number: \" .. tostring(qps))\n" +
            "redis.log(redis.LOG_NOTICE, \"ARGV[2] (timeWindow): \" .. tostring(ARGV[2]) .. \", as number: \" .. tostring(timeWindow))\n" +
            "redis.log(redis.LOG_NOTICE, \"ARGV[3] (now): \" .. tostring(ARGV[3]) .. \", as number: \" .. tostring(now))\n" +
            "redis.log(redis.LOG_NOTICE, \"ARGV[4] (waitable): \" .. tostring(ARGV[4]))\n" +
            "redis.log(redis.LOG_NOTICE, \"ARGV[5] (timeout): \" .. tostring(ARGV[5]) .. \", as number: \" .. tostring(timeout))\n" +
            "\n" +
            "-- 参数验证\n" +
            "if qps == nil or timeWindow == nil or now == nil then\n" +
            "    redis.log(redis.LOG_ERR, \"Error: Required parameters are nil\")\n" +
            "    return -1\n" +
            "end\n" +
            "\n" +
            "-- 获取桶数据\n" +
            "local bucket = redis.call('hmget', key, 'tokens', 'lastRefillTime')\n" +
            "local tokens = bucket[1]\n" +
            "local lastRefillTime = bucket[2]\n" +
            "\n" +
            "redis.log(redis.LOG_NOTICE, \"tostring tokens: \" .. tostring(tokens))\n" +
            "redis.log(redis.LOG_NOTICE, \"tostring lastRefillTime: \" .. tostring(lastRefillTime))\n"+
            "\n" +
            "-- 正确的空值检查：Redis返回的nil在Lua中是false\n" +
            "if tokens == false or tokens == nil or tostring(tokens) == 'nil' then\n" +
            "    redis.log(redis.LOG_NOTICE, \"Initializing new bucket for key: \" .. key)\n" +
            "    tokens = qps\n" +
            "    lastRefillTime = now\n" +
            "    redis.call('hmset', key, 'tokens', tostring(tokens), 'lastRefillTime', tostring(lastRefillTime))\n" +
            "    redis.call('expire', key, timeWindow * 2)\n" +
            "    redis.log(redis.LOG_NOTICE, \"New bucket created - tokens: \" .. tostring(tokens) .. \", lastRefillTime: \" .. tostring(lastRefillTime))\n" +
            "else\n" +
            "    redis.log(redis.LOG_NOTICE, \"Processing existing bucket\")\n" +
            "    -- 转换现有数据\n" +
            "    tokens = safe_tonumber(tokens)\n" +
            "    lastRefillTime = safe_tonumber(lastRefillTime)\n" +
            "    \n" +
            "    redis.log(redis.LOG_NOTICE, \"Converted bucket data - tokens: \" .. tostring(tokens) .. \", lastRefillTime: \" .. tostring(lastRefillTime))\n" +
            "    \n" +
            "    -- 检查转换结果\n" +
            "    if tokens == nil or lastRefillTime == nil then\n" +
            "        redis.log(redis.LOG_ERR, \"Error: Invalid bucket data, reinitializing\")\n" +
            "        tokens = qps\n" +
            "        lastRefillTime = now\n" +
            "        redis.call('hmset', key, 'tokens', tostring(tokens), 'lastRefillTime', tostring(lastRefillTime))\n" +
            "        redis.call('expire', key, timeWindow * 2)\n" +
            "    else\n" +
            "        local timePassed = now - lastRefillTime\n" +
            "        redis.log(redis.LOG_NOTICE, \"Time passed since last refill: \" .. tostring(timePassed) .. \" seconds\")\n" +
            "        \n" +
            "        if timePassed > 0 then\n" +
            "            local refillTokens = timePassed * qps / timeWindow\n" +
            "            redis.log(redis.LOG_NOTICE, \"Refill tokens calculated: \" .. tostring(refillTokens))\n" +
            "            tokens = math.min(qps, tokens + refillTokens)\n" +
            "            lastRefillTime = now\n" +
            "            redis.call('hmset', key, 'tokens', tostring(tokens), 'lastRefillTime', tostring(lastRefillTime))\n" +
            "            redis.log(redis.LOG_NOTICE, \"Bucket refilled - new tokens: \" .. tostring(tokens))\n" +
            "        end\n" +
            "    end\n" +
            "end\n" +
            "\n" +
            "redis.log(redis.LOG_NOTICE, \"Final token count: \" .. tostring(tokens))\n" +
            "\n" +
            "-- 检查令牌并处理请求\n" +
            "if tokens >= 1 then\n" +
            "    tokens = tokens - 1\n" +
            "    redis.call('hset', key, 'tokens', tostring(tokens))\n" +
            "    redis.log(redis.LOG_NOTICE, \"Request ALLOWED - tokens decreased to: \" .. tostring(tokens))\n" +
            "    return 1\n" +
            "else\n" +
            "    redis.log(redis.LOG_NOTICE, \"Request LIMITED - insufficient tokens\")\n" +
            "    return 0\n" +
            "end";


    public static final String COUNT_RATE_LIMIT_SCRIPT = "local key = KEYS[1]\n" +
            "\n" +
            "-- 安全的数字转换函数\n" +
            "local function safe_tonumber(str)\n" +
            "    if str == nil then\n" +
            "        return nil\n" +
            "    end\n" +
            "    if type(str) == \"number\" then\n" +
            "        return str\n" +
            "    end\n" +
            "    if type(str) == \"string\" then\n" +
            "        -- 清理字符串：移除空格和特殊字符\n" +
            "        local cleaned = string.gsub(str, \"%s+\", \"\")\n" +
            "        cleaned = string.gsub(cleaned, \"[^%d%.]\", \"\")\n" +
            "        return tonumber(cleaned)\n" +
            "    end\n" +
            "    return nil\n" +
            "end\n" +
            "\n" +
            "-- 转换参数\n" +
            "local qps = safe_tonumber(ARGV[1])\n" +
            "local timeWindow = safe_tonumber(ARGV[2])\n" +
            "local now = safe_tonumber(ARGV[3])       -- 当前时间戳\n" +
            "\n" +
            "-- 记录转换结果（英文）\n" +
            "redis.log(redis.LOG_NOTICE, \"Converted parameters:\")\n" +
            "redis.log(redis.LOG_NOTICE, \"qps: \" .. safe_tonumber(qps))\n" +
            "redis.log(redis.LOG_NOTICE, \"timeWindow: \" .. safe_tonumber(timeWindow))\n" +
            "redis.log(redis.LOG_NOTICE, \"now: \" .. safe_tonumber(now))\n" +
            "\n" +
            "-- 参数验证\n" +
            "if qps == nil then\n" +
            "    redis.log(redis.LOG_WARNING, \"Error: qps is invalid\")\n" +
            "    return -1\n" +
            "end\n" +
            "\n" +
            "if timeWindow == nil then\n" +
            "    redis.log(redis.LOG_WARNING, \"Error: timeWindow is invalid\")\n" +
            "    return -1\n" +
            "end\n" +
            "if now == nil then\n" +
            "    redis.log(redis.LOG_WARNING, \"Error: now is invalid\")\n" +
            "    return -1\n" +
            "end\n" +
            "\n" +
            "-- 计算当前窗口的开始时间\n" +
            "local window_start = math.floor(now / timeWindow) * timeWindow\n" +
            "local window_key = key .. \":\" .. tostring(window_start)\n" +
            "\n" +
            "redis.log(redis.LOG_NOTICE, \"window_key: \" .. window_key)\n" +
            "\n" +
            "-- 使用窗口特定的key，自然过期\n" +
            "local current = redis.call('incr', window_key)\n" +
            "if current == 1 then\n" +
            "    -- 第一次设置过期时间，设置为窗口长度的2倍\n" +
            "    -- 确保在窗口切换期间数据不会丢失\n" +
            "    redis.call('expire', window_key, timeWindow * 2)\n" +
            "end\n" +
            "\n" +
            "if current > qps then\n" +
            "    return 0  -- 超过限制\n" +
            "else\n" +
            "    return 1  -- 允许\n" +
            "end";
}
