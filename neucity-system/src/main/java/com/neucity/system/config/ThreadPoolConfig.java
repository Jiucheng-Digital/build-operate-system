package com.neucity.system.config;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.async.LazyTraceThreadPoolTaskExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2025/9/30 15:32
 */
@Configuration
public class ThreadPoolConfig {

    @Value("${thread.pool.core-size:10}")
    private int corePoolSize;

    @Value("${thread.pool.max-size:100}")
    private int maxPoolSize;


    @Autowired
    private Tracer tracer;

    @Autowired
    private SpanNamer spanNamer;

    @Autowired
    private BeanFactory beanFactory;


    @Bean("threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor asyncPool() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("taskExecutor-pool-%d").build();
        // 核心线程数：10
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        // 最大线程数：100
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        // 空闲线程存活时间：600秒（10分钟）
        threadPoolTaskExecutor.setKeepAliveSeconds(600);
        // 线程池关闭时的等待时间：30秒
        threadPoolTaskExecutor.setAwaitTerminationSeconds(30);
        // 任务队列容量：10240
        threadPoolTaskExecutor.setQueueCapacity(10240);
        // 绑定线程工厂
        threadPoolTaskExecutor.setThreadFactory(namedThreadFactory);
        // 任务装饰器（追踪上下文传递）
        threadPoolTaskExecutor.setTaskDecorator(new TraceableDecorator(this.tracer,this.spanNamer));
        // 拒绝策略：丢弃最旧任务
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        // 关闭时等待任务完成
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        // 最多等待30秒
        threadPoolTaskExecutor.setAwaitTerminationSeconds(30);
        threadPoolTaskExecutor.initialize();
        return new LazyTraceThreadPoolTaskExecutor(beanFactory, threadPoolTaskExecutor);
    }

}
