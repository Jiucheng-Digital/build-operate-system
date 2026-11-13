package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 外部系统主动上报事件的请求。
 */
@Data
public class EventCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Positive(message = "驱动ID必须为正数")
    private Long driveId;

    @Positive(message = "设备ID必须为正数")
    private Long deviceId;

    /** 事件类型编码，不是 sys_event 的主键。 */
    @NotBlank(message = "事件编码不能为空")
    @Size(max = 128, message = "事件编码不能超过128个字符")
    private String eventId;

    @NotBlank(message = "事件名称不能为空")
    @Size(max = 128, message = "事件名称不能超过128个字符")
    private String eventName;

    /** 支持本地日期时间和 ISO 8601 时间；不传时使用当前时间。 */
    private String eventTime;

    @NotBlank(message = "事件等级不能为空")
    @Pattern(regexp = "low|middle|high", message = "事件等级只能为low、middle或high")
    private String level;

    /** 扩展信息，沿用事件模型的 JSON 字符串格式。 */
    private String extend;

    @Size(max = 32, message = "厂商不能超过32个字符")
    private String manufacturer;

    @Size(max = 32, message = "设备分类不能超过32个字符")
    private String classify;
}
