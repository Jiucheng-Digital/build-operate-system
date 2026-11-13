package com.neucity.system.dto;

import com.neucity.common.core.domain.dto.BaseDTO;
import lombok.Data;

import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/7/27 15:05
 */
@Data
public class UserByDeviceTypeDTO extends BaseDTO {

    List<String> deviceTypes;

}
