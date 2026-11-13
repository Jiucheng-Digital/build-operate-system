///*
// * Copyright (c) 2022. CloudStar.Co.Ltd. All rights reserved.
// */
//
//package com.cqxf.common.dubbo.config;
//
//import cn.hutool.core.util.BooleanUtil;
//import cn.hutool.core.util.StrUtil;
//import org.apache.dubbo.config.MetadataReportConfig;
//import org.apache.dubbo.config.RegistryConfig;
//import org.apache.dubbo.config.SslConfig;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.core.env.Environment;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
///**
// * DubboConfig
// *
// * @author ChaoHong.Mao
// */
//@Configuration
//public class DubboConfig {
//
//    // Dubbo TLSv支持协议，注：不支持TLSv1.3
//    private static final List<String> PROTOCOLS = Collections.singletonList("TLSv1.2");
//
//    // Dubbo TLSv支持密码格式
//    private static final List<String> CIPHERS = Arrays.asList(
//            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
//            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
//            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
//            "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
//            "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
//            "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256",
//            "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
//            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"
//    );
//
//    @Autowired
//    private Environment env;
//
//
//    /**
//     * 元数据报告配置
//     *
//     * @return {@link MetadataReportConfig}
//     */
//    @Bean
//    @Primary
//    public MetadataReportConfig metadataReportConfig() {
//        MetadataReportConfig metadataReportConfig = new MetadataReportConfig();
//        String nacosAddr = env.getProperty(NACOS_ADDR);
//        String nacosUsername = env.getProperty(NACOS_USERNAME);
//        String nacosPassword = decodeCipher(env.getProperty(NACOS_CIPHER));
//        String addr = "nacos://" + nacosAddr;
//        metadataReportConfig.setAddress(addr);
//        metadataReportConfig.setUsername(nacosUsername);
//        metadataReportConfig.setPassword(nacosPassword);
//        return metadataReportConfig;
//    }
//
//    /**
//     * 注册表配置，替代默认注册，加上 nacos 密码的解密
//     *
//     * @return {@link RegistryConfig}
//     */
//    @Bean
//    @Primary
//    public RegistryConfig registryConfig() {
//        RegistryConfig registryConfig = new RegistryConfig();
//        String nacosAddr = env.getProperty(NACOS_ADDR);
//        String nacosUsername = env.getProperty(NACOS_USERNAME);
//        String nacosPassword = decodeCipher(env.getProperty(NACOS_CIPHER));
//        String cacheFile = env.getProperty(NACOS_CACHE_FILE);
//        String addr = "nacos://" + nacosAddr + "?username=" + nacosUsername + "&password=" + nacosPassword;
//        registryConfig.setAddress(addr);
//        registryConfig.setFile(cacheFile);
//        registryConfig.setCheck(false);
//        registryConfig.setSimplified(true);
//        registryConfig.setExtraKeys("ssl-enabled");
//        registryConfig.setUsername(nacosUsername);
//        registryConfig.setPassword(nacosPassword);
//
//        return registryConfig;
//    }
//
//
//    /**
//     * dubbo tls 配置
//     *
//     * @author Young
//     */
//    @Bean
//    public SslConfig sslConfig() {
//        SslConfig sslConfig = new SslConfig();
//        // 是否开启ssl，false为不开启
//        if (!BooleanUtil.toBoolean(env.getProperty(SSL_ENABLE))) {
//            return sslConfig;
//        }
//
//        // 信任证书
//        String collectionCert = env.getProperty(SSL_CERT_COLLECTION);
//        // 证书
//        String cert = env.getProperty(SSL_CERT);
//        // 私钥
//        String priKey = env.getProperty(SSL_KEY);
//        // 私钥密码
//        String password = env.getProperty(SSL_CERT_CIPHER);;
//
//        // server
//        // 服务端信任证书
//        sslConfig.setServerTrustCertCollectionPath(collectionCert);
//        // 服务端证书
//        sslConfig.setServerKeyCertChainPath(cert);
//        // 服务端私钥
//        sslConfig.setServerPrivateKeyPath(priKey);
//
//        // consumer
//        // 客户端信任证书
//        sslConfig.setClientTrustCertCollectionPath(collectionCert);
//        // 客户端证书
//        sslConfig.setClientKeyCertChainPath(cert);
//        // 客户端私钥
//        sslConfig.setClientPrivateKeyPath(priKey);
//
//        if (StrUtil.isNotEmpty(password)) {
//            // 服务端私钥密码
//            sslConfig.setServerKeyPassword(password);
//            // 客户端私钥密码
//            sslConfig.setClientKeyPassword(password);
//        }
//
//        // 支持协议TLSv1.2
//        sslConfig.setProtocols(PROTOCOLS);
//
//        // 支持密码格式
//        sslConfig.setCiphers(CIPHERS);
//
//        return sslConfig;
//    }
//}
