package com.neucity.build.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 门禁点反控请求参数。
 */
@Data
public class DoorDoControlDTO {
    /**
     * 门禁点唯一标识集合，最大支持10个门禁点。
     */
    @NotNull(message = "门禁点唯一标识集合不能为空")
    private List<String> doorIndexCodes;

    /**
     * 反控类型；0-常开，1-门闭，2-门开，3-常闭。
     */
    @NotNull(message = "反控类型不能为空")
    private Integer controlType;

    /**
     *  驱动ID
     */
    @NotNull(message = "驱动ID不能为空")
    private Long driveId;

    /**
     * 用户ID AI调用会传
     */
    private Long userId;
}
