package com.neucity.common.mybatisplus.incrementer;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.neucity.common.mybatisplus.util.IdWorker;
import org.springframework.stereotype.Component;

/**
 * 自定义mybatis id生成策略
 *
 * @author : yoga
 */
@Component
public class CustomerIdGenerator implements IdentifierGenerator {

    @Override
    public Number nextId(Object entity) {
        return IdWorker.generateId();
    }


}
