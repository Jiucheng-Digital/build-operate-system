package com.neucity.common.core.model.dto.websocket;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/4/1 14:36
 */
@Data
public class FormAndToDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> topic;

    private String message;

}
