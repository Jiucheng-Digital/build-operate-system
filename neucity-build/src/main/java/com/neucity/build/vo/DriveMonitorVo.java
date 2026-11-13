package com.neucity.build.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 驱动容器监控信息
 *
 * @author ShuPF
 * @date 2026/6/10 17:20
 */
@Data
public class DriveMonitorVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 驱动ID */
    private Long driveId;

    /** 驱动名称 */
    private String driveName;

    /** 驱动状态 */
    private String driveStatus;

    /** 驱动调用地址 */
    private String driveUrl;

    /** 监控时间 */
    private Date monitorTime;

    /** 容器ID列表 */
    private List<String> containerIds;

    /** 容器名称列表 */
    private List<String> containerNames;

    /** CPU资源 */
    private CpuResource cpu;

    /** 内存资源 */
    private MemoryResource memory;

    /** 存储资源 */
    private StorageResource storage;

    /** 网络资源 */
    private NetworkResource network;

    @Data
    public static class CpuResource implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 状态 */
        private String status;

        /** 使用率 */
        private String usageRate;

        /** 内核版本 */
        private String kernelVersion;

        /** 架构 */
        private String architecture;
    }

    @Data
    public static class MemoryResource implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 状态 */
        private String status;

        /** 已使用 / 总内存 */
        private String usedTotalMemory;

        /** 已使用内存 */
        private String usedMemory;

        /** 总内存 */
        private String totalMemory;

        /** 缓存 */
        private String cacheMemory;

        /** 可用内存 */
        private String availableMemory;

        /** 使用率 */
        private String usageRate;
    }

    @Data
    public static class StorageResource implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 状态 */
        private String status;

        /** 已使用 / 总存储 */
        private String usedTotalStorage;

        /** 数据分区 */
        private String dataPartition;

        /** 数据分区已使用 */
        private String dataUsedStorage;

        /** 数据分区总容量 */
        private String dataTotalStorage;

        /** 日志分区 */
        private String logPartition;

        /** 日志分区已使用 */
        private String logUsedStorage;

        /** 日志分区总容量 */
        private String logTotalStorage;

        /** 使用率 */
        private String usageRate;
    }

    @Data
    public static class NetworkResource implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 状态 */
        private String status;

        /** IP地址 */
        private String ipAddress;

        /** 网络接口 */
        private String interfaceName;

        /** 上行速度 */
        private String uploadSpeed;

        /** 下行速度 */
        private String downloadSpeed;

        /** 网络状态 */
        private String networkStatus;

        /** 驱动容器状态 */
        private String containerStatus;
    }
}
