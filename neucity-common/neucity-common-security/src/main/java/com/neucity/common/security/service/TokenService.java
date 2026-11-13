package com.neucity.common.security.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import com.neucity.common.core.constant.CacheConstants;
import com.neucity.common.core.constant.SecurityConstants;
import com.neucity.common.core.context.SecurityContextHolder;
import com.neucity.common.core.exception.ServiceException;
import com.neucity.common.core.model.LoginUser;
import com.neucity.common.core.model.dto.SysUserDTO;
import com.neucity.common.core.utils.JwtUtils;
import com.neucity.common.core.utils.ServletUtils;
import com.neucity.common.core.utils.StringUtils;
import com.neucity.common.core.utils.ip.IpUtils;
import com.neucity.common.core.utils.uuid.IdUtils;
import com.neucity.common.redis.service.RedisService;
import com.neucity.common.security.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * token验证处理
 *
 * @author shupf
 */
@Component
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Autowired
    private RedisService redisService;

    protected static final long MILLIS_SECOND = 1000;

    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    private final static long TOKEN_EXPIRE_TIME = CacheConstants.EXPIRATION;

    private final static String ACCESS_TOKEN = CacheConstants.LOGIN_TOKEN_KEY;

    private final static Long TOKEN_REFRESH_THRESHOLD_MINUTES = CacheConstants.REFRESH_TIME * MILLIS_MINUTE;

    /**
     * 创建令牌
     */
    public Map<String, Object> createToken(LoginUser loginUser) {
        String token = IdUtils.fastUUID();
        Long userId = loginUser.getSysUser().getUserId();
        String userName = loginUser.getSysUser().getUserName();
        loginUser.setToken(token);
        loginUser.setUserid(userId);
        loginUser.setUsername(userName);
        loginUser.setIpaddr(IpUtils.getIpAddr());
        refreshToken(loginUser);

        // Jwt存储信息
        Map<String, Object> claimsMap = new HashMap<String, Object>();
        claimsMap.put(SecurityConstants.USER_KEY, token);
        claimsMap.put(SecurityConstants.DETAILS_USER_ID, userId);
        claimsMap.put(SecurityConstants.DETAILS_USERNAME, userName);

        // 接口返回信息
        Map<String, Object> rspMap = new HashMap<String, Object>();
        rspMap.put("access_token", JwtUtils.createToken(claimsMap));
        rspMap.put("expires_in", TOKEN_EXPIRE_TIME);
        return rspMap;
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser() {
        return getLoginUser(ServletUtils.getRequest());
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(HttpServletRequest request) {
        // 获取请求携带的令牌
        String token = SecurityUtils.getToken(request);
        return getLoginUser(token);
    }

    /**
     * 获取用户身份信息
     *
     * @return 用户信息
     */
    public LoginUser getLoginUser(String token) {
        LoginUser user = null;
        try {
            if (StringUtils.isNotEmpty(token)) {
                String userKey = JwtUtils.getUserKey(token);
                Object object = redisService.getCacheObject(getTokenKey(userKey));
                if (object == null) {
                    log.error("TokenService.getLoginUser object is null, 登录已过期");
                    return null;
                }

                if (object instanceof LoginUser) {
                    user = (LoginUser) object;
                } else {
                    user = JSON.parseObject(object.toString(), LoginUser.class);
                }

                return user;
            }
        } catch (Exception e) {
            log.error("获取用户信息异常: ", e);
        }
        return user;
    }

    /**
     * 设置用户身份信息
     */
    public void setLoginUser(LoginUser loginUser) {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken())) {
            refreshToken(loginUser);
        }
    }

    public LoginUser setLoginUser(Long userId) {
        // 获取登录信息
        Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");

        List<LoginUser> redisUsers = new ArrayList<>();
        for (String key : keys) {
            LoginUser user = null;
            try {
                user = redisService.getCacheObject(key);
            } catch (Exception e) {
                user = JSONUtil.toBean(redisService.getCacheObject(key).toString(), LoginUser.class);
            }

            if (user != null && user.getUserid() != null && user.getUserid() - userId == 0) {
                redisUsers.add(user);
            }
        }

        if (CollectionUtil.isEmpty(redisUsers)) {
            throw new ServiceException("用户未登录");
        }

        redisUsers = redisUsers.stream().sorted(Comparator.comparing(LoginUser::getLoginTime).reversed()).collect(Collectors.toList());

        LoginUser loginUser = redisUsers.get(0);
        log.info("TokenService.setLoginUser-{} 登录用户信息", JSONUtil.toJsonStr(loginUser));
        SecurityUtils.setLoginUser(loginUser);
        SecurityContextHolder.setUserId(loginUser.getUserid() + "");
        SecurityContextHolder.setUserName(loginUser.getUsername());
        SecurityContextHolder.setUserKey(loginUser.getToken());

        return loginUser;
    }

    /**
     * 删除用户缓存信息
     */
    public void delLoginUser(String token) {
        if (StringUtils.isNotEmpty(token)) {
            String userkey = JwtUtils.getUserKey(token);
            redisService.deleteObject(getTokenKey(userkey));
        }
    }

    /**
     * 验证令牌有效期，相差不足120分钟，自动刷新缓存
     *
     * @param loginUser
     */
    public void verifyToken(LoginUser loginUser) {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= TOKEN_REFRESH_THRESHOLD_MINUTES) {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新令牌有效期
     *
     * @param loginUser 登录信息
     */
    public void refreshToken(LoginUser loginUser) {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + TOKEN_EXPIRE_TIME * MILLIS_MINUTE);
        // 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken());
        redisService.setCacheObject(userKey, loginUser, TOKEN_EXPIRE_TIME, TimeUnit.MINUTES);
    }

    public String getUserLoginDevice(Long userId) {
        StringBuilder loginDevice = new StringBuilder();
        Collection<String> keys = redisService.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        for (String key : keys) {
            LoginUser user = null;
            try {
                user = redisService.getCacheObject(key);
            } catch (Exception e) {
                user = JSONUtil.toBean(redisService.getCacheObject(key).toString(), LoginUser.class);
            }

            if (user == null) {
                continue;
            }

            SysUserDTO sysUser = user.getSysUser();
            if (sysUser == null) {
                continue;
            }

            String deviceName = user.getLoginDevice();
            if (sysUser.getUserId() - userId == 0 && StringUtils.isNotBlank(deviceName) && !loginDevice.toString().contains(deviceName)) {
                loginDevice.append(deviceName).append(",");
            }
        }

        if (StringUtils.isNotBlank(loginDevice.toString())) {
            loginDevice = new StringBuilder(loginDevice.substring(0, loginDevice.length() - 1));
        }

        return loginDevice.toString();
    }

    private String getTokenKey(String token) {
        return ACCESS_TOKEN + token;
    }
}