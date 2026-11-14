# 智慧建筑空间管理操作系统

三个应用统一使用 `dev`，从本机 Nacos 读取数据库、Redis 和网关配置。

| 应用 | HTTP 端口 | 上下文 | Dubbo 端口 |
| --- | --- | --- | --- |
| neucity-system | 6002 | /api/system | 20889 |
| neucity-build | 6007 | /api/build | 消费 system 服务 |
| neucity-gateway | 8086 | / | — |

## 构建与启动

在工作区根目录执行：

```powershell
.\stop.ps1
mvn -B clean verify
.\start.ps1 -Profile dev -JavaHome 'D:\shupf\java\JDK\jdk1.0.8'
```

启动脚本默认使用 `dev`，依次启动 system、build、gateway，等待每个服务健康状态为 `UP`。日志与 PID 保存在 `.codex/run`；失败时停止本次已启动的进程。重新构建或改用 IDEA 调试前，先停止脚本启动的同名服务，避免 HTTP 和 Dubbo 端口冲突。

编译目标为 Java 8。脚本兼容 Windows PowerShell 5、PowerShell 7、JDK 8 和 JDK 21。Java 9 以上运行 system/build 时需要 `--add-opens=java.base/java.lang=ALL-UNNAMED`，脚本会自动添加。

## Nacos dev 配置

控制台为 `http://127.0.0.1:8848/nacos`。三个应用的默认配置分组均为 `DEFAULT_GROUP`，读取以下 Data ID：

| Data ID | 用途 |
| --- | --- |
| application-dev.yml | 公共自动配置及 Feign 配置 |
| neucity-system-dev.yml | 系统数据库、Redis、文件和接口文档配置 |
| neucity-build-dev.yml | 建筑数据库、Redis、驱动及数据中心同步配置 |
| neucity-gateway-dev.yml | Redis、服务路由、鉴权白名单 |

业务配置以 Nacos 为准，`dev` 启动不加载 `.codex/local`。本地 `bootstrap.yml` 保留应用名、端口和 Nacos/Dubbo 启动连接信息。可通过 `NACOS_SERVER_ADDR` 和 `NACOS_CONFIG_GROUP` 切换地址与分组，切换前需确保目标分组已有完整配置。

本机 Kafka 地址 `192.168.0.230:9092` 当前不可达，已在 Nacos 的建筑 dev 配置中设置 `data-center.sync.enabled: false` 和 `cleanup-cron: "-"`，暂停发件箱投递及清理。现有待发送记录保留；Kafka 恢复后可在 Nacos 中重新开启投递，修改清理 cron 后需重启服务。其他设备相关功能仍需要对应驱动和设备服务可达。

IDEA 中可以直接运行 `SystemApplication`、`BuildApplication` 和 `GatewayApplication`，默认均为 `dev`。JDK 8 无需额外 VM 参数；Java 9 以上运行 system/build 时需要上述 `--add-opens` 参数。

## 访问与验证

- 网关接口文档：`http://localhost:8086/swagger-ui.html`
- 系统健康：`http://localhost:6002/api/system/actuator/health`
- 建筑健康：`http://localhost:6007/api/build/actuator/health`
- 网关健康：`http://localhost:8086/actuator/health`

网关通过 Nacos 服务发现转发 `/api/system/**` 和 `/api/build/**`，业务请求继续执行令牌校验。接口文档路径已加入 Nacos 网关 dev 白名单。

本次构建日志为 `.codex/dev-build.log`；配置修改前的备份保存在 `.codex/backups/before-dev-startup-20261030`，其中包含数据库密码，不应提交。
