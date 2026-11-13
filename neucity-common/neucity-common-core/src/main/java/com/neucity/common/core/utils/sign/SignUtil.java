package com.neucity.common.core.utils.sign;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import com.neucity.common.core.exception.ServiceException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 *  数据加解密及验签
 *
 * @author ShuPF
 * @date 2025/10/29 9:54
 */
public class SignUtil {


    /**
     *  构建数据加密参数
     * @param appSecret appSecret
     * @param appKey appKey
     * @param dataJsonStr Json数据
     * @return 加密数据
     */
    public static Map<String, Object> buildParamMap( String appSecret, String appKey, String dataJsonStr) {
        String timestamp = System.currentTimeMillis() + "";
        String nonce = IdUtil.fastSimpleUUID();
        String data = encryptData(appSecret, dataJsonStr);
        String sign = SecureUtil.hmacSha256(appSecret).digestHex(StrUtil.format("apiKey={}&data={}&nonce={}&ts={}", appKey, data, nonce, timestamp)).toUpperCase();
        Map<String, Object> paramMap = new HashMap<>(5);

        paramMap.put("apiKey", appKey);
        paramMap.put("ts", timestamp);
        paramMap.put("nonce", nonce);
        paramMap.put("sign", sign);
        paramMap.put("data", data);
        return paramMap;
    }

    public static String signVerify(String appSecret, String appKey, Map<String, Object> paramMap) {
        String timestamp = paramMap.get("ts").toString();
        String nonce = paramMap.get("nonce").toString();
        String sign = paramMap.get("sign").toString();
        String data = paramMap.get("data").toString();

        if (StringUtils.isBlank(timestamp)) {
            throw new ServiceException("签名参数异常");
        }

        long cz = System.currentTimeMillis() - Long.parseLong(timestamp);
        cz = cz / 1000;
        if (cz > 60) {
            throw new ServiceException("签名已过期");
        }

        if (StringUtils.isBlank(data) || data.length() <= 5) {
            throw new ServiceException("data参数异常");
        }

        String mySign = SecureUtil.hmacSha256(appSecret).digestHex(StrUtil.format("apiKey={}&data={}&nonce={}&ts={}", appKey, data, nonce, timestamp)).toUpperCase();
        if (!sign.equals(mySign)) {
            throw new ServiceException("签名验证失败");
        }

        return decryptData(appSecret, data);
    }

    /**
     *  加密数据
     */
    public static String encryptData(String appSecret, String data) {
        return SmUtil.sm4(appSecret.substring(0, 16).getBytes()).encryptBase64(data) + AKSKGenerator.generateAK().substring(0, 5);
    }

    /**
     *  加密数据 16进制
     */
    public static String encryptDataHex(String appSecret, String data) {
        return SmUtil.sm4(appSecret.substring(0, 16).getBytes()).encryptHex(data) + AKSKGenerator.generateAK().substring(0, 5);
    }

    /**
     *  解密数据
     */
    public static String decryptData(String appSecret, String data) {
        data = data.substring(0, data.length() - 5);
        return SmUtil.sm4(appSecret.substring(0, 16).getBytes()).decryptStr(data);
    }

    public static void main(String[] args) {
        String appSecret = "WzxjrhnVKPL+L26T1uWMIlzNk2D/6N18";

        String encryptData = encryptData(appSecret, "12345678");

        System.out.println(encryptData);
        System.out.println(decryptData(appSecret, encryptData));
    }

}
