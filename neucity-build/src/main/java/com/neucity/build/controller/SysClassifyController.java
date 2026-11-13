package com.neucity.build.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.build.mapper.SysClassifyMapper;
import com.neucity.common.core.web.controller.BaseController;
import com.neucity.common.core.web.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/8 17:04
 */
@RestController
@RequestMapping("/classify")
public class SysClassifyController extends BaseController {

    @Autowired
    private SysClassifyMapper sysClassifyMapper;

    @RequestMapping("/list")
    public AjaxResult list() {
        return success(sysClassifyMapper.selectList(new QueryWrapper<>()));
    }

}
