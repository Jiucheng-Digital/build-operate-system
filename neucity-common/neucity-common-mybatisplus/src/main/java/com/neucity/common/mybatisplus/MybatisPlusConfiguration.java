package com.neucity.common.mybatisplus;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.neucity.common.mybatisplus.incrementer.CustomerIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * MybatisPlusConfiguration
 *
 * @author cd
 * @date 2025/7/4 16:57
 */
@Slf4j
@EnableTransactionManagement
@Configuration
@MapperScan(basePackages = "com.neucity.**.mapper", sqlSessionFactoryRef = "sqlSessionFactory")
public class MybatisPlusConfiguration {

    // 注入配置开关，默认值false防止配置未配置时报错
    @Value("${mybatis.sql-log-enabled:false}")
    private Boolean sqlLogEnabled;

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                               MybatisPlusProperties properties) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        Resource[] mapperLocations = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/**/*.xml");
        factory.setMapperLocations(mapperLocations);

        // 配置MyBatis-Plus全局配置
        GlobalConfig globalConfig = properties.getGlobalConfig();
        // globalConfig.setMetaObjectHandler(metaObjectHandler);
        globalConfig.setIdentifierGenerator(new CustomerIdGenerator());

        // 设置MyBatis-Plus配置
        MybatisConfiguration configuration = new MybatisConfiguration();

        log.info("MyBatisPlusConfiguration-sqlLogEnabled:{}", sqlLogEnabled);

        // 判断是否为开发/测试环境
        if (sqlLogEnabled) {
            configuration.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        } else {
            configuration.setLogImpl(org.apache.ibatis.logging.nologging.NoLoggingImpl.class);
        }

        configuration.setMapUnderscoreToCamelCase(true);

        // 添加分页插件
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        paginationInnerInterceptor.setOverflow(true);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);

        factory.setConfiguration(configuration);
        factory.setGlobalConfig(globalConfig);
        factory.setPlugins(interceptor);

        return factory.getObject();
    }

}
