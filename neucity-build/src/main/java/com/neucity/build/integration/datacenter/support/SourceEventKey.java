package com.neucity.build.integration.datacenter.support;

import com.neucity.build.domain.SysEventRecord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 生成稳定的上游事件指纹，避免驱动查询窗口重叠造成重复告警。 */
public final class SourceEventKey {

    private SourceEventKey() {
    }

    public static String of(SysEventRecord event) {
        String value = part(event.getDriveId()) + '|' + part(event.getManufacturer()) + '|'
                + part(event.getDeviceId()) + '|' + part(event.getEventId()) + '|'
                + part(event.getEventTime()) + '|' + part(event.getEventName()) + '|'
                + part(event.getExtend());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JRE 不支持 SHA-256", exception);
        }
    }

    private static String part(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
