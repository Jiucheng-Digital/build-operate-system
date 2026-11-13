package com.neucity.build.vo;

import com.neucity.build.domain.SysEvent;
import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/11 16:57
 */
@Data
public class SysEventVo extends SysEvent implements Serializable {

    private String doorIndexCode;

    private String classifyName;

    private String manufacturer;

    private String deviceName;
}
