package com.neucity.build.vo;

import com.neucity.build.domain.SysEventRecord;
import lombok.Data;

import java.io.Serializable;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/11 16:57
 */
@Data
public class SysEventRecordVo extends SysEventRecord implements Serializable {

    private String doorIndexCode;

    private String classifyName;

    private String manufacturer;

    private String deviceName;
}
