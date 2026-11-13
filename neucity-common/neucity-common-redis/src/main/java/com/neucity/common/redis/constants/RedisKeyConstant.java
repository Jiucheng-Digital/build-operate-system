package com.neucity.common.redis.constants;

/**
 * redis key 静态变量
 *
 * @author ShuPF
 * @date 2026/3/9 11:01
 */
public class RedisKeyConstant {

    /**
     * 获取用户ai模型的key
     * @param userId
     * @return key
     */
    public static String getUserAiModelKey(Long userId) {
        return "user:aiModelToken:" + userId;
    }

    /**
     * 获取机器人工作状态的key
     * @param userId 机器人用户ID
     * @return key 机器人工作状态
     */
    public static String getRobotWorkStateKey(Long userId) {
        return "robot:workState:" + userId;
    }

    /**
     * 获取机器人任务分数的key
     * @return key 机器人任务分数
     */
    public static String getRobotTaskScoreKey() {
        return "robot:taskScore";
    }

    /**
     *  获取机器人查房KEY
     */
    public static String getRobotWardRound(Long robotId) {
        return "robot:wardRound:" + robotId;
    }

    /**
     * 获取机器人运行数据KEY
     */
    public static String gerRobotRunData() {
        return "robot:runData:";
    }

    public static String getRobotPositionKey() {
        return "robot:position:";
    }
}
