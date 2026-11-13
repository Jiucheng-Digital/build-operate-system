package com.neucity.build.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONNull;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.neucity.build.dto.DoorDoControlDTO;
import com.neucity.build.dto.FaceCallbackDTO;
import com.neucity.build.dto.StartStreamDTO;
import com.neucity.build.service.DoorDriveService;
import com.neucity.common.annotation.annotation.Log;
import com.neucity.common.annotation.enums.BusinessType;
import com.neucity.common.core.exception.ServiceException;
import com.neucity.common.core.utils.StringUtils;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import com.neucity.common.redis.service.RedisService;
import com.neucity.common.security.annotation.RepeatSubmit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/30 9:48
 */
@Slf4j
@RestController
@RequestMapping("/ai/face")
public class AiFaceController extends BaseController {

    private static final String STREAM_KEY = "face:stream:";
    private static final int DEFAULT_SAMPLE_INTERVAL_SEC = 2;

    @Value("${ai.stream.face-group-id}")
    private String groupId;

    @Value("${ai.stream.callback}")
    private String callback;

    @Value("${ai.stream.api-url}")
    private String apiUrl;

    @Autowired
    private RedisService redisService;

    @Autowired
    private DoorDriveService driveService;

    @Log(title = "设备-开启流监控", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/start")
    public AjaxResult start(@RequestBody @Validated StartStreamDTO dto) {
        JSONObject registerBody = JSONUtil.createObj()
                .set("url", dto.getUrl())
                .set("face_group_id", groupId)
                .set("name", dto.getName());
        JSONObject stream = request(Method.POST, streamApi("/streams"), registerBody);
        String streamId = stream.getStr("id");
        if (StringUtils.isBlank(streamId)) {
            throw new ServiceException("注册人脸识别流失败");
        }

        String key = STREAM_KEY + streamId;
        String key2 = STREAM_KEY + dto.getDeviceId();
        redisService.setCacheObject(key2, streamId);
        redisService.setCacheMapValue(key, "doorIndexCode", dto.getDoorIndexCode());
        redisService.setCacheMapValue(key, "doorDriveId", dto.getDoorDriveId());

        JSONObject startBody = JSONUtil.createObj()
                .set("sample_interval_sec", DEFAULT_SAMPLE_INTERVAL_SEC)
                .set("face_group_id", groupId)
                .set("callback_url", callback);
        JSONObject startResult = request(Method.POST, streamApi("/streams/" + streamId + "/start"), startBody);
        return AjaxResult.success("开始人脸识别", toPlainObject(startResult));
    }

    @Log(title = "设备-关闭流监控", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @PostMapping("/stop/{deviceId}")
    public AjaxResult stop(@PathVariable("deviceId") Long deviceId) {
        String key = STREAM_KEY + deviceId;
        String streamId = redisService.getCacheObject(key);
        if (StringUtils.isBlank(streamId)) {
            throw new ServiceException("设备未开启人脸识别流");
        }

        request(Method.POST, streamApi("/streams/" + streamId + "/stop"), null);
        request(Method.DELETE, streamApi("/streams/" + streamId), null);
        redisService.deleteObject(key);
        redisService.deleteObject(STREAM_KEY + streamId);
        return success("停止人脸识别");
    }

    @PostMapping("/callback")
    public AjaxResult callback(@RequestBody FaceCallbackDTO dto) {
        if (dto == null) {
            log.warn("AiFaceController.callback 参数错误");
            return success();
        }

        log.info("AiFaceController.callback 被调用，参数: {}", JSONUtil.toJsonStr(dto));

        if (CollectionUtil.isNotEmpty(dto.getFaces())) {
            String key = STREAM_KEY + dto.getStream_id();
            String doorIndexCode = redisService.getCacheMapValue(key, "doorIndexCode");
            Long doorDriveId = redisService.getCacheMapValue(key, "doorDriveId");
            if (StringUtils.isBlank(doorIndexCode) || doorDriveId == null) {
                log.warn("AiFaceController.callback 找不到门禁编码或驱动");
                return success();
            }

            log.info("AiFaceController.callback 识别到人脸，门禁编码: {}, 驱动ID: {}", doorIndexCode, doorDriveId);
            // 调用门禁开门
            DoorDoControlDTO request = new DoorDoControlDTO();
            request.setDriveId(doorDriveId);
            request.setDoorIndexCodes(Collections.singletonList(doorIndexCode));
            request.setControlType(2);
            driveService.doorDoControl(request);
        }

        return success();
    }

    @PostMapping("/callbackTest")
    public AjaxResult callbackTest(@RequestBody String message) {
        log.info("AiFaceController.callbackTest：params: {}", message);
        return AjaxResult.success();
    }

    private JSONObject request(Method method, String url, JSONObject body) {
        log.info("AiFaceController.request method: {}, url: {}, body: {}", method, url, body);
        HttpResponse response = HttpUtil.createRequest(method, url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(body == null ? "" : body.toString())
                .execute();

        String responseBody = response.body();
        log.info("AiFaceController.request response status: {}, body: {}", response.getStatus(), responseBody);

        if (!response.isOk()) {
            throw new ServiceException("调用人脸识别流接口失败");
        }
        if (StringUtils.isBlank(responseBody)) {
            return JSONUtil.createObj();
        }
        return JSONUtil.parseObj(responseBody);
    }

    private String streamApi(String path) {
        return apiUrl.replaceAll("/+$", "") + path;
    }

    private Object toPlainObject(Object value) {
        if (value == null || JSONNull.NULL.equals(value)) {
            return null;
        }
        if (value instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) value;
            Map<String, Object> map = new LinkedHashMap<>();
            for (String key : jsonObject.keySet()) {
                map.put(key, toPlainObject(jsonObject.get(key)));
            }
            return map;
        }
        if (value instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) value;
            List<Object> list = new ArrayList<>();
            for (Object item : jsonArray) {
                list.add(toPlainObject(item));
            }
            return list;
        }
        return value;
    }

}
