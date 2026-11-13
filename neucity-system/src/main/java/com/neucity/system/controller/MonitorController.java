package com.neucity.system.controller;

import com.neucity.common.annotation.annotation.Log;
import com.neucity.common.annotation.enums.BusinessType;
import com.neucity.common.core.constant.CacheConstants;
import com.neucity.common.core.utils.StringUtils;
import com.neucity.common.core.utils.poi.ExcelUtil;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.core.web.page.TableDataInfo;
import com.neucity.system.domain.Server;
import com.neucity.system.domain.SysCache;
import com.neucity.system.domain.SysLogininfor;
import com.neucity.system.domain.SysOperLog;
import com.neucity.system.service.ISysLogininforService;
import com.neucity.system.service.ISysOperLogService;
import com.neucity.system.service.SysPasswordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/2/3 10:00
 */
@RequestMapping("/monitor")
@RestController
public class MonitorController extends BaseController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ISysOperLogService operLogService;

    @Autowired
    private ISysLogininforService logininforService;

    @Autowired
    private SysPasswordService passwordService;

    private final static List<SysCache> caches = new ArrayList<SysCache>();{
        caches.add(new SysCache(CacheConstants.LOGIN_TOKEN_KEY, "用户信息"));
        caches.add(new SysCache(CacheConstants.SYS_CONFIG_KEY, "配置信息"));
        caches.add(new SysCache(CacheConstants.SYS_DICT_KEY, "数据字典"));
        caches.add(new SysCache(CacheConstants.CAPTCHA_CODE_KEY, "验证码"));
        caches.add(new SysCache(CacheConstants.REPEAT_SUBMIT_KEY, "防重提交"));
        caches.add(new SysCache(CacheConstants.RATE_LIMIT_KEY, "限流处理"));
        caches.add(new SysCache(CacheConstants.PWD_ERR_CNT_KEY, "密码错误次数"));
    }

    @GetMapping(value = "server")
    public AjaxResult getServerInfo() throws Exception {
        Server server = new Server();
        server.copyTo();
        return AjaxResult.success(server);
    }

    /** ================================ 缓存监控 ===================================================*/
    @GetMapping(value = "/cache")
    public AjaxResult getCacheInfo() throws Exception {
        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info());
        Properties commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info("commandstats"));
        Object dbSize = redisTemplate.execute((RedisCallback<Object>) connection -> connection.dbSize());

        Map<String, Object> result = new HashMap<>(3);
        result.put("info", info);
        result.put("dbSize", dbSize);

        List<Map<String, String>> pieList = new ArrayList<>();
        commandStats.stringPropertyNames().forEach(key -> {
            Map<String, String> data = new HashMap<>(2);
            String property = commandStats.getProperty(key);
            data.put("name", StringUtils.removeStart(key, "cmdstat_"));
            data.put("value", StringUtils.substringBetween(property, "calls=", ",usec"));
            pieList.add(data);
        });
        result.put("commandStats", pieList);
        return AjaxResult.success(result);
    }

    @GetMapping("/cache/getNames")
    public AjaxResult cache()
    {
        return AjaxResult.success(caches);
    }

    @GetMapping("/cache/getKeys/{cacheName}")
    public AjaxResult getCacheKeys(@PathVariable String cacheName)
    {
        Set<String> cacheKeys = redisTemplate.keys(cacheName + "*");
        return AjaxResult.success(new TreeSet<>(cacheKeys));
    }

    @GetMapping("/cache/getValue/{cacheName}/{cacheKey}")
    public AjaxResult getCacheValue(@PathVariable String cacheName, @PathVariable String cacheKey)
    {
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        SysCache sysCache = new SysCache(cacheName, cacheKey, cacheValue);
        return AjaxResult.success(sysCache);
    }

    @DeleteMapping("/cache/clearCacheName/{cacheName}")
    public AjaxResult clearCacheName(@PathVariable String cacheName)
    {
        Collection<String> cacheKeys = redisTemplate.keys(cacheName + "*");
        redisTemplate.delete(cacheKeys);
        return AjaxResult.success();
    }

    @DeleteMapping("/cache/clearCacheKey/{cacheKey}")
    public AjaxResult clearCacheKey(@PathVariable String cacheKey)
    {
        redisTemplate.delete(cacheKey);
        return AjaxResult.success();
    }

    @DeleteMapping("/cache/clearCacheAll")
    public AjaxResult clearCacheAll()
    {
        Collection<String> cacheKeys = redisTemplate.keys("*");
        redisTemplate.delete(cacheKeys);
        return AjaxResult.success();
    }

    /** ============================================ 操作日志 ============================================*/
    @GetMapping("/operlog/list")
    public TableDataInfo operlogList(SysOperLog operLog)
    {
        startPage();
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        return getDataTable(list);
    }

    @Log(title = "操作日志", businessType = BusinessType.EXPORT)
    @PostMapping("/operlo/export")
    public void operlogExport(HttpServletResponse response, SysOperLog operLog)
    {
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        ExcelUtil<SysOperLog> util = new ExcelUtil<SysOperLog>(SysOperLog.class);
        util.exportExcel(response, list, "操作日志");
    }

    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/operlo/{operIds}")
    public AjaxResult operlogRemove(@PathVariable Long[] operIds)
    {
        return toAjax(operLogService.deleteOperLogByIds(operIds));
    }

    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/operlo/clean")
    public AjaxResult operlogClean()
    {
        operLogService.cleanOperLog();
        return success();
    }

    /** ============================================ 登录日志 ============================================*/

    @GetMapping("/logininfor/list")
    public TableDataInfo logininforList(SysLogininfor logininfor) {
        startPage();
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        return getDataTable(list);
    }

    @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @PostMapping("/logininfor/export")
    public void logininforExport(HttpServletResponse response, SysLogininfor logininfor) {
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        ExcelUtil<SysLogininfor> util = new ExcelUtil<SysLogininfor>(SysLogininfor.class);
        util.exportExcel(response, list, "登录日志");
    }

    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/logininfor/{infoIds}")
    public AjaxResult logininforRemove(@PathVariable Long[] infoIds) {
        return toAjax(logininforService.deleteLogininforByIds(infoIds));
    }

    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/logininfor/clean")
    public AjaxResult logininforClean() {
        logininforService.cleanLogininfor();
        return success();
    }

    @Log(title = "账户解锁", businessType = BusinessType.OTHER)
    @GetMapping("/logininfor/unlock/{userName}")
    public AjaxResult logininforUnlock(@PathVariable("userName") String userName) {
        passwordService.clearLoginRecordCache(userName);
        return success();
    }
}
