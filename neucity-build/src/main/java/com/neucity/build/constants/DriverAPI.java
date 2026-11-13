package com.neucity.build.constants;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/8 17:45
 */

public interface DriverAPI {

    /**
     *  获取配置
     */
    public static String CONFIG_URL = "/api/v1/config";

    /**
     *  搜索设备
     */
    public static String DEVICE_SEARCH_URL = "/api/v1/drive/device/search";

    public static String QUERY_EVENT = "/api/v1/drive/device/events";

    /**
     *  获取API
     */
    public static String DEVICE_API = "/api/v1/config/api";

    class DoorApi {

        /**
         *  门禁点反控
         */
        public static String OPEN_URL = "/api/v1/drive/door/doControl";

        /**
         * POST 查询门禁状态
         */
        public static String QUERY_STATE = "/api/v1/drive/door/states";

    }

    class MotorApi {

        /**
         *  云台控制 POST
         */
        public static String PTZS_CONTROL_URL = "/api/v1/drive/cameras/ptzs/controlling";

        /**
         *  获取监控点预览取流URLv2
         */
        public static String GET_CAMERA_URL = "/api/v1/drive/cameras/previewURLs";

    }

    class Lighting {

        /**
         *  控制所有传感器 POST
         */
        public static String CONTROL_ALL_SENSOR = "/api/v1/drive/sensor/control/all";

        /**
         *  控制传感器 POST
         */
        public static String CONTROL_SENSOR = "/api/v1/drive/sensor/control";
    }
}
