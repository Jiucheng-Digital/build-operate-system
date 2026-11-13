package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 门禁点反控请求参数。
 */
@Data
public class DoorDoControlByNameDTO {
    /**

    /**
     * 反控类型；0-常开，1-门闭，2-门开，3-常闭。
     */
    @NotNull(message = "反控类型不能为空")
    private Integer controlType;

    /**
     *  设备名称
     */
    @NotNull(message = "设备名称不能为空")
    private String name;

}
