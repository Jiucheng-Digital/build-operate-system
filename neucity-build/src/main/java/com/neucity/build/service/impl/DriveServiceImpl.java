package com.neucity.build.service.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neucity.build.constants.DriverAPI;
import com.neucity.build.domain.DriveInfo;
import com.neucity.build.dto.DriveCommonDTO;
import com.neucity.build.dto.DriveDTO;
import com.neucity.build.dto.DriveListDTO;
import com.neucity.build.dto.UpdateConfigDTO;
import com.neucity.build.mapper.DeviceInfoMapper;
import com.neucity.build.service.DriveDeviceService;
import com.neucity.build.service.DriveService;
import com.neucity.build.vo.DriveInfoVo;
import com.neucity.build.vo.DriveMonitorVo;
import com.neucity.build.vo.DriveResultVO;
import com.neucity.build.websocket.CommonMsgWebsocket;
import com.neucity.common.core.enums.CommonWebsoketTypeEnum;
import com.neucity.common.core.exception.ServiceException;
import com.neucity.common.core.model.dto.websocket.CommonMsgVo;
import com.neucity.common.core.utils.PageUtils;
import com.neucity.common.security.utils.SecurityUtils;
import com.neucity.common.security.utils.WebUserUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 描述
 *
 * @author ShuPF
 * @date 2026/6/3 16:26
 */
@Slf4j
@Service
public class DriveServiceImpl implements DriveService {

    @Value("${drive.start-port:20000}")
    private Integer startPort;

    @Value("${drive.server-ip:127.0.0.1}")
    private String serverIp;

    @Value("${drive.install-path:/data/drives}")
    private String installPath;

    @Autowired
    private CommonMsgWebsocket commonMsgWebsocket;

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Autowired
    private DriveDeviceService driveDeviceService;

    /**
     *  驱动上传
     *   1. 驱动只支持zip格式
     *   2. 创建驱动目录 （installPath + 文件名称（去除特殊字符））
     *   3. 解压驱动文件到驱动目录
     *   4. 根据 startPort 判断端口是否被占用，如果没用占用，修改zip中 docker-compose.yml 文件的宿主机端口, 替换 HOST_MACHINE_PORT
     *   5. 启动驱动 执行命令 docker-compose up -d
     *   6. 查询服务docker状态，判断是否启动成功
     */
    @Override
    public void uploadDrive(DriveDTO dto) {
        log.info("uploadDrive 启动上传驱动，参数：{}", dto);
        long count = deviceInfoMapper.selectCount(new QueryWrapper<DriveInfo>()
                .eq("classify_code", dto.getClassifyCode())
                .eq("type", dto.getType())
                .ne(dto.getId() != null, "id", dto.getId())
        );
        if (count > 0) {
            log.error("该厂商下已存在该类型驱动");
            throw new ServiceException("该厂商下已存在该类型驱动");
        }

        if (dto.getId() != null) {
            this.removeDrive(dto.getId());
        }

        Long userId = null;
        DriveUploadFile uploadFile = null;

        try {
            log.info("uploadDrive 上传安装包-开始 path: [{}]", installPath);
            uploadFile = validateUploadFile(dto);
            userId = getUploadUserId(dto);
            String driveName = sanitizeDriveName(FilenameUtils.getBaseName(uploadFile.getOriginalFilename()));
            Path driveDir = Paths.get(installPath, driveName).toAbsolutePath().normalize();

            createDriveDirectory(driveDir);
            unzip(uploadFile, driveDir);

            log.info("uploadDrive 上传安装包-成功 path: [{}]", driveDir);

            Path composeFile = driveDir.resolve("docker-compose.yml");
            if (!Files.exists(composeFile)) {
                log.error("uploadDrive 安转失败 缺少docker-compose.yml文件");
                this.sendMsg(userId, "install_error", "驱动包缺少docker-compose.yml文件");
                return;
            }

            int hostPort = findAvailablePort(startPort);
            replaceHostPort(composeFile, hostPort);
            log.info("uploadDrive 驱动【{}】使用宿主机端口【{}】启动，安装目录：【{}】", driveName, hostPort, driveDir);

            // 驱动上传成功
            this.sendMsg(userId, "upload_success", "");

            log.info("uploadDrive 驱动启动-开始");
            CommandResult upResult = executeDockerComposeCommand(driveDir, "up", "-d");
            if (upResult.exitCode != 0) {
                log.error("uploadDrive 驱动启动-失败");
                this.sendMsg(userId, "start_error", "启动失败：" + upResult.output);
                return;
            }

            // 驱动安装成功
            this.sendMsg(userId, "install_success", "");

            List<String> containerIds = getContainerIds(driveDir);
            if (containerIds.isEmpty()) {
                log.error("uploadDrive 驱动启动-失败 未查询到docker容器");
                this.sendMsg(userId, "start_error", "启动失败：未查询到docker容器");
                return;
            }

            for (String containerId : containerIds) {
                if (!isContainerRunning(containerId)) {
                    log.error("uploadDrive 驱动启动-失败 容器未正常运行 contain：{}", containerId);
                    this.sendMsg(userId, "start_error",
                            "启动失败：容器未正常运行，contain" + containerId);
                }
            }

            log.info("uploadDrive 驱动启动-成功");

            DriveInfo driveInfo = new DriveInfo();
            driveInfo.setName(driveName);
            driveInfo.setInstallPath(driveDir.toString());
            driveInfo.setUrl("http://" + serverIp + ":" + hostPort);
            driveInfo.setPort(hostPort);
            driveInfo.setType(dto.getType());
            driveInfo.setClassifyCode(dto.getClassifyCode());
            driveInfo.setStatus("install_success");
            driveInfo.setVersion(dto.getVersion());
            driveInfo.setRemark(dto.getRemark());
            if (dto.getId() == null) {
                WebUserUtil.prepareInsertParams(driveInfo);
                deviceInfoMapper.insert(driveInfo);
            } else {
                dto.setId(dto.getId());
                WebUserUtil.prepareUpdateParams(driveInfo);
                deviceInfoMapper.updateById(driveInfo);
            }

            // 加载完成
            this.sendMsg(userId, "start_success", "");

            // 异步同步设备
            CompletableFuture.runAsync(() -> {
                driveDeviceService.deviceSync(driveInfo.getId());
            });

        } catch (ServiceException e) {
            log.error("驱动上传失败", e);
            this.sendMsg(userId, "upload_error", "驱动上传失败: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("驱动上传失败", e);
            this.sendMsg(userId, "upload_error", "驱动上传失败: " + e.getMessage());
        } finally {
            log.info("uploadDrive 删除临时文件");
            if (uploadFile != null) {
                uploadFile.deleteTempFile();
            }
        }
    }

    @Override
    public List<DriveInfoVo> list(DriveListDTO dto) {
        PageUtils.startPage();
        return deviceInfoMapper.selectDeviceInfoList(dto);
    }

    @Override
    public List<DriveInfoVo> listByClassify(String classifyCode) {

        return deviceInfoMapper.selectVoList(new QueryWrapper<DriveInfo>().eq("classify_code", classifyCode));
    }

    @Override
    public DriveMonitorVo monitor(Long driveId) {
        if (driveId == null) {
            return buildHostMonitor();
        }

        DriveInfo driveInfo = deviceInfoMapper.selectById(driveId);
        if (driveInfo == null) {
            throw new ServiceException("驱动不存在");
        }

        DriveMonitorVo monitorVo = new DriveMonitorVo();
        monitorVo.setDriveId(driveInfo.getId());
        monitorVo.setDriveName(driveInfo.getName());
        monitorVo.setDriveStatus(driveInfo.getStatus());
        monitorVo.setDriveUrl(driveInfo.getUrl());
        monitorVo.setMonitorTime(new Date());

        Path driveDir = getDriveDir(driveInfo);
        List<String> containerIds = getMonitorContainerIds(driveInfo, driveDir);
        List<ContainerStats> firstStats = getContainerStats(containerIds);
        sleepQuietly(1000L);
        List<ContainerStats> containerStats = getContainerStats(containerIds);
        ContainerInspectInfo inspectInfo = getContainerInspectInfo(containerIds);
        String containerStatus = buildContainerStatus(containerIds);

        monitorVo.setContainerIds(containerIds);
        monitorVo.setContainerNames(getContainerNames(containerStats));
        monitorVo.setCpu(buildCpuResource(containerStats, containerStatus));
        monitorVo.setMemory(buildMemoryResource(containerStats, containerStatus));
        monitorVo.setStorage(buildStorageResource(containerStats, inspectInfo, containerStatus));
        monitorVo.setNetwork(buildNetworkResource(firstStats, containerStats, inspectInfo, containerStatus));
        return monitorVo;
    }

    private DriveMonitorVo buildHostMonitor() {
        DriveMonitorVo monitorVo = new DriveMonitorVo();
        monitorVo.setDriveName("宿主机");
        monitorVo.setDriveStatus("running");
        monitorVo.setMonitorTime(new Date());
        monitorVo.setContainerIds(Collections.emptyList());
        monitorVo.setContainerNames(Collections.emptyList());
        monitorVo.setCpu(buildHostCpuResource());
        monitorVo.setMemory(buildHostMemoryResource());
        monitorVo.setStorage(buildHostStorageResource());
        monitorVo.setNetwork(buildHostNetworkResource());
        return monitorVo;
    }

    @Override
    public int removeDrive(Long driveId) {
        DriveInfo driveInfo = deviceInfoMapper.selectById(driveId);
        if (driveInfo == null) {
            throw new ServiceException("驱动不存在");
        }

        Path driveDir = Paths.get(driveInfo.getInstallPath()).toAbsolutePath().normalize();
        // 停止驱动
        CommandResult upResult = null;
        try {
            upResult = executeDockerComposeCommand(driveDir, "down");
            if (upResult.exitCode != 0) {
                throw new ServiceException("驱动停止失败: " + upResult.output);
            }
        } catch (Exception e) {
            log.error("驱动停止失败", e);
            throw new ServiceException("驱动停止失败");
        }

        driveInfo.setStatus("uninstall");
        WebUserUtil.prepareUpdateParams(driveInfo);

        return  deviceInfoMapper.updateById(driveInfo);
    }

    @Override
    public int installDrive(Long driveId) {
        DriveInfo driveInfo = deviceInfoMapper.selectById(driveId);
        if (driveInfo == null) {
            throw new ServiceException("驱动不存在");
        }

        Path driveDir = Paths.get(driveInfo.getInstallPath()).toAbsolutePath().normalize();
        CommandResult upResult = null;
        try {
            upResult = executeDockerComposeCommand(driveDir, "up", "-d");
            if (upResult.exitCode != 0) {
                throw new ServiceException("驱动启动失败: " + upResult.output);
            }

            List<String> containerIds = getContainerIds(driveDir);
            if (containerIds.isEmpty()) {
                throw new ServiceException("启动失败：未查询到docker容器");
            }

            for (String containerId : containerIds) {
                if (!isContainerRunning(containerId)) {
                    throw new ServiceException("启动失败：容器未正常运行，contain" + containerId);
                }
            }
        } catch (Exception e) {
            log.error("驱动启动失败", e);
            throw new ServiceException("驱动启动失败");
        }

        driveInfo.setStatus("install_success");
        WebUserUtil.prepareUpdateParams(driveInfo);

        return deviceInfoMapper.updateById(driveInfo);
    }

    @Override
    public Object getConfig(DriveCommonDTO dto) {
        DriveInfo driveInfo = deviceInfoMapper.selectById(dto.getDriveId());

        if (driveInfo == null) {
            log.error("driveInfo is null");
            throw new ServiceException("请选择正确的驱动");
        }

        String url = driveInfo.getUrl();
        if (StringUtils.isBlank(url)) {
            log.error("url is null");
            throw new ServiceException("请选择正确的驱动");
        }

        url = url + DriverAPI.CONFIG_URL;

        log.info("getConfig url: {}", url);
        String result = HttpUtil.get(url);
        log.info("getConfig result: {}", result);

        DriveResultVO driveResultVO = JSONUtil.toBean(result, DriveResultVO.class);
        if (!driveResultVO.isSuccess()) {
            throw new ServiceException(driveResultVO.getMessage());
        }

        return driveResultVO.getData();
    }

    @Override
    public void updateConfig(UpdateConfigDTO dto) {
        DriveInfo driveInfo = deviceInfoMapper.selectById(dto.getDriveId());

        if (driveInfo == null) {
            log.error("driveInfo is null");
            throw new ServiceException("请选择正确的驱动");
        }

        String url = driveInfo.getUrl();
        if (StringUtils.isBlank(url)) {
            log.error("url is null");
            throw new ServiceException("请选择正确的驱动");
        }

        url = url + DriverAPI.CONFIG_URL;

        log.info("updateConfig url: {}", url);
        String result = HttpUtil.createRequest(Method.PUT, url).body(dto.getConfigJson()).execute().body();
        log.info("updateConfig result: {}", result);
        JSONObject object = JSONUtil.parseObj(result);
        if (!object.getBool("success")) {
            throw new ServiceException(object.getStr("message"));
        }
    }

    @Override
    public Object getApi(DriveCommonDTO dto) {
        DriveInfo driveInfo = deviceInfoMapper.selectById(dto.getDriveId());

        if (driveInfo == null) {
            log.error("getApi is null");
            throw new ServiceException("请选择正确的驱动");
        }

        String url = driveInfo.getUrl();
        if (StringUtils.isBlank(url)) {
            log.error("getApi  is null");
            throw new ServiceException("请选择正确的驱动");
        }

        url = url + DriverAPI.DEVICE_API;

        log.info("getApi url: {}", url);
        String result = HttpUtil.get(url);
        log.info("getApi result: {}", result);

        DriveResultVO driveResultVO = JSONUtil.toBean(result, DriveResultVO.class);
        if (!driveResultVO.isSuccess()) {
            throw new ServiceException(driveResultVO.getMessage());
        }

        return driveResultVO.getData();
    }

    private DriveMonitorVo.CpuResource buildHostCpuResource() {
        DriveMonitorVo.CpuResource cpuResource = new DriveMonitorVo.CpuResource();
        double usageRate = getHostCpuUsageRate();
        cpuResource.setUsageRate(formatPercent(usageRate));
        cpuResource.setStatus(buildResourceStatus(usageRate));
        cpuResource.setKernelVersion(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        cpuResource.setArchitecture(System.getProperty("os.arch"));
        return cpuResource;
    }

    private DriveMonitorVo.MemoryResource buildHostMemoryResource() {
        DriveMonitorVo.MemoryResource memoryResource = new DriveMonitorVo.MemoryResource();
        Map<String, Long> memInfo = readHostMemInfo();
        long total = memInfo.getOrDefault("MemTotal", getOperatingSystemMemory("getTotalPhysicalMemorySize"));
        long available = memInfo.getOrDefault("MemAvailable", getOperatingSystemMemory("getFreePhysicalMemorySize"));
        long cache = memInfo.getOrDefault("Cached", 0L) + memInfo.getOrDefault("SReclaimable", 0L);
        long used = Math.max(total - available, 0L);
        double usageRate = total <= 0 ? 0D : used * 100D / total;

        memoryResource.setStatus(buildResourceStatus(usageRate));
        memoryResource.setUsedMemory(formatBytes(used));
        memoryResource.setTotalMemory(formatBytes(total));
        memoryResource.setUsedTotalMemory(formatBytes(used) + " / " + formatBytes(total));
        memoryResource.setCacheMemory(formatBytes(cache));
        memoryResource.setAvailableMemory(formatBytes(available));
        memoryResource.setUsageRate(formatPercent(usageRate));
        return memoryResource;
    }

    private DriveMonitorVo.StorageResource buildHostStorageResource() {
        DriveMonitorVo.StorageResource storageResource = new DriveMonitorVo.StorageResource();
        File dataFile = resolveExistingFile(installPath, "/data", ".");
        File logFile = resolveExistingFile("/var/log", "logs", ".");

        long dataTotal = dataFile.getTotalSpace();
        long dataUsed = Math.max(dataTotal - dataFile.getUsableSpace(), 0L);
        long logTotal = logFile.getTotalSpace();
        long logUsed = Math.max(logTotal - logFile.getUsableSpace(), 0L);
        double usageRate = dataTotal <= 0 ? 0D : dataUsed * 100D / dataTotal;

        storageResource.setStatus(buildStorageStatus(usageRate));
        storageResource.setUsedTotalStorage(formatBytes(dataUsed) + " / " + formatBytes(dataTotal));
        storageResource.setDataUsedStorage(formatBytes(dataUsed));
        storageResource.setDataTotalStorage(formatBytes(dataTotal));
        storageResource.setDataPartition(dataFile.getAbsolutePath() + " (" + formatBytes(dataUsed) + " 已使用)");
        storageResource.setLogUsedStorage(formatBytes(logUsed));
        storageResource.setLogTotalStorage(formatBytes(logTotal));
        storageResource.setLogPartition(logFile.getAbsolutePath() + " (" + formatBytes(logUsed) + " 已使用)");
        storageResource.setUsageRate(formatPercent(usageRate));
        return storageResource;
    }

    private DriveMonitorVo.NetworkResource buildHostNetworkResource() {
        DriveMonitorVo.NetworkResource networkResource = new DriveMonitorVo.NetworkResource();
        NetworkInterface networkInterface = getPrimaryNetworkInterface();
        if (networkInterface == null) {
            networkResource.setStatus("异常");
            networkResource.setNetworkStatus("未连接");
            networkResource.setUploadSpeed("0 bps");
            networkResource.setDownloadSpeed("0 bps");
            networkResource.setContainerStatus("宿主机");
            return networkResource;
        }

        NetworkSnapshot firstSnapshot = readHostNetworkSnapshot(networkInterface.getName());
        sleepQuietly(1000L);
        NetworkSnapshot secondSnapshot = readHostNetworkSnapshot(networkInterface.getName());
        long intervalMillis = firstSnapshot == null || secondSnapshot == null
                ? 1000L : Math.max(secondSnapshot.timestamp - firstSnapshot.timestamp, 1L);
        long rxBytes = firstSnapshot == null || secondSnapshot == null
                ? 0L : Math.max(secondSnapshot.rxBytes - firstSnapshot.rxBytes, 0L);
        long txBytes = firstSnapshot == null || secondSnapshot == null
                ? 0L : Math.max(secondSnapshot.txBytes - firstSnapshot.txBytes, 0L);

        networkResource.setStatus("正常");
        networkResource.setIpAddress(getIpAddress(networkInterface));
        networkResource.setInterfaceName(networkInterface.getName());
        networkResource.setDownloadSpeed(formatBitRate(rxBytes, intervalMillis));
        networkResource.setUploadSpeed(formatBitRate(txBytes, intervalMillis));
        networkResource.setNetworkStatus("已连接");
        networkResource.setContainerStatus("宿主机");
        return networkResource;
    }

    private double getHostCpuUsageRate() {
        CpuSnapshot firstSnapshot = readHostCpuSnapshot();
        if (firstSnapshot != null) {
            sleepQuietly(200L);
            CpuSnapshot secondSnapshot = readHostCpuSnapshot();
            if (secondSnapshot != null) {
                long total = secondSnapshot.total - firstSnapshot.total;
                long idle = secondSnapshot.idle - firstSnapshot.idle;
                if (total > 0) {
                    return Math.max(0D, Math.min(100D, (total - idle) * 100D / total));
                }
            }
        }

        try {
            Object operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            Object value = operatingSystemMXBean.getClass().getMethod("getSystemCpuLoad").invoke(operatingSystemMXBean);
            if (value instanceof Double && (Double) value >= 0D) {
                return Math.max(0D, Math.min(100D, (Double) value * 100D));
            }
        } catch (Exception e) {
            log.debug("获取宿主机CPU使用率失败", e);
        }
        return 0D;
    }

    private CpuSnapshot readHostCpuSnapshot() {
        Path statPath = Paths.get("/proc/stat");
        if (!Files.exists(statPath)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(statPath, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (StringUtils.isBlank(line) || !line.startsWith("cpu ")) {
                return null;
            }
            String[] values = line.trim().split("\\s+");
            long idle = parseLong(values, 4) + parseLong(values, 5);
            long total = 0L;
            for (int i = 1; i < values.length; i++) {
                total += parseLong(values, i);
            }
            return new CpuSnapshot(total, idle);
        } catch (Exception e) {
            log.debug("读取宿主机/proc/stat失败", e);
            return null;
        }
    }

    private Map<String, Long> readHostMemInfo() {
        Map<String, Long> memInfo = new HashMap<>();
        Path memInfoPath = Paths.get("/proc/meminfo");
        if (!Files.exists(memInfoPath)) {
            return memInfo;
        }
        try (BufferedReader reader = Files.newBufferedReader(memInfoPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length < 2) {
                    continue;
                }
                String value = parts[1].replace("kB", "").trim();
                if (StringUtils.isNumeric(value)) {
                    memInfo.put(parts[0], Long.valueOf(value) * 1024L);
                }
            }
        } catch (Exception e) {
            log.debug("读取宿主机/proc/meminfo失败", e);
        }
        return memInfo;
    }

    private long getOperatingSystemMemory(String methodName) {
        try {
            Object operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            Object value = operatingSystemMXBean.getClass().getMethod(methodName).invoke(operatingSystemMXBean);
            if (value instanceof Long) {
                return (Long) value;
            }
        } catch (Exception e) {
            log.debug("获取宿主机内存信息失败：{}", methodName, e);
        }
        return 0L;
    }

    private File resolveExistingFile(String... paths) {
        for (String path : paths) {
            if (StringUtils.isBlank(path)) {
                continue;
            }
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        }
        return new File(".");
    }

    private NetworkInterface getPrimaryNetworkInterface() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                if (StringUtils.isNotBlank(getIpAddress(networkInterface))) {
                    return networkInterface;
                }
            }
        } catch (Exception e) {
            log.debug("获取宿主机网络接口失败", e);
        }
        return null;
    }

    private String getIpAddress(NetworkInterface networkInterface) {
        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
        while (addresses.hasMoreElements()) {
            InetAddress address = addresses.nextElement();
            if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                return address.getHostAddress();
            }
        }
        return null;
    }

    private NetworkSnapshot readHostNetworkSnapshot(String interfaceName) {
        Path devPath = Paths.get("/proc/net/dev");
        if (!Files.exists(devPath)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(devPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.startsWith(interfaceName + ":")) {
                    continue;
                }
                String[] parts = line.substring(line.indexOf(":") + 1).trim().split("\\s+");
                if (parts.length >= 16) {
                    return new NetworkSnapshot(Long.valueOf(parts[0]), Long.valueOf(parts[8]), System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            log.debug("读取宿主机/proc/net/dev失败", e);
        }
        return null;
    }

    private DriveMonitorVo.CpuResource buildCpuResource(List<ContainerStats> containerStats, String containerStatus) {
        DriveMonitorVo.CpuResource cpuResource = new DriveMonitorVo.CpuResource();
        double usageRate = 0D;
        for (ContainerStats stats : containerStats) {
            usageRate += stats.getCpuPercent();
        }
        cpuResource.setUsageRate(formatPercent(usageRate));
        cpuResource.setStatus("运行中".equals(containerStatus) ? buildResourceStatus(usageRate) : containerStatus);
        cpuResource.setKernelVersion("容器运行时");
        cpuResource.setArchitecture("Docker");
        return cpuResource;
    }

    private DriveMonitorVo.MemoryResource buildMemoryResource(List<ContainerStats> containerStats, String containerStatus) {
        DriveMonitorVo.MemoryResource memoryResource = new DriveMonitorVo.MemoryResource();
        long used = 0L;
        long total = 0L;
        for (ContainerStats stats : containerStats) {
            used += stats.getMemUsage();
            total += stats.getMemLimit();
        }
        long available = Math.max(total - used, 0L);
        double usageRate = total <= 0 ? 0D : used * 100D / total;

        memoryResource.setStatus("运行中".equals(containerStatus) ? buildResourceStatus(usageRate) : containerStatus);
        memoryResource.setUsedMemory(formatBytes(used));
        memoryResource.setTotalMemory(formatBytes(total));
        memoryResource.setUsedTotalMemory(formatBytes(used) + " / " + formatBytes(total));
        memoryResource.setCacheMemory("0 B");
        memoryResource.setAvailableMemory(formatBytes(available));
        memoryResource.setUsageRate(formatPercent(usageRate));
        return memoryResource;
    }

    private DriveMonitorVo.StorageResource buildStorageResource(List<ContainerStats> containerStats,
                                                               ContainerInspectInfo inspectInfo,
                                                               String containerStatus) {
        DriveMonitorVo.StorageResource storageResource = new DriveMonitorVo.StorageResource();
        long blockRead = 0L;
        long blockWrite = 0L;
        for (ContainerStats stats : containerStats) {
            blockRead += stats.getBlockRead();
            blockWrite += stats.getBlockWrite();
        }
        long writableSize = inspectInfo.getWritableSize();
        long rootFsSize = inspectInfo.getRootFsSize();
        long total = rootFsSize <= 0L ? writableSize : rootFsSize;
        long used = writableSize;
        double usageRate = total <= 0 ? 0D : used * 100D / total;

        storageResource.setStatus("运行中".equals(containerStatus) ? buildStorageStatus(usageRate) : containerStatus);
        storageResource.setUsedTotalStorage(formatBytes(used) + " / " + formatBytes(total));
        storageResource.setDataUsedStorage(formatBytes(writableSize));
        storageResource.setDataTotalStorage(formatBytes(total));
        storageResource.setDataPartition("容器可写层 (" + formatBytes(writableSize) + " 已使用)");
        storageResource.setLogUsedStorage(formatBytes(blockWrite));
        storageResource.setLogTotalStorage(formatBytes(blockRead + blockWrite));
        storageResource.setLogPartition("容器Block IO (读 " + formatBytes(blockRead) + " / 写 " + formatBytes(blockWrite) + ")");
        storageResource.setUsageRate(formatPercent(usageRate));
        return storageResource;
    }

    private DriveMonitorVo.NetworkResource buildNetworkResource(List<ContainerStats> firstStats,
                                                               List<ContainerStats> secondStats,
                                                               ContainerInspectInfo inspectInfo,
                                                               String containerStatus) {
        DriveMonitorVo.NetworkResource networkResource = new DriveMonitorVo.NetworkResource();
        long intervalMillis = calculateStatsInterval(firstStats, secondStats);
        long rxBytes = Math.max(sumNetworkRx(secondStats) - sumNetworkRx(firstStats), 0L);
        long txBytes = Math.max(sumNetworkTx(secondStats) - sumNetworkTx(firstStats), 0L);

        networkResource.setStatus("运行中".equals(containerStatus) ? "正常" : containerStatus);
        networkResource.setIpAddress(inspectInfo.getIpAddress());
        networkResource.setInterfaceName(inspectInfo.getNetworkName());
        networkResource.setDownloadSpeed(formatBitRate(rxBytes, intervalMillis));
        networkResource.setUploadSpeed(formatBitRate(txBytes, intervalMillis));
        networkResource.setNetworkStatus(StringUtils.isBlank(inspectInfo.getIpAddress()) ? "未连接" : "已连接");
        networkResource.setContainerStatus(containerStatus);
        return networkResource;
    }

    private Path getDriveDir(DriveInfo driveInfo) {
        if (StringUtils.isBlank(driveInfo.getInstallPath())) {
            throw new ServiceException("驱动安装目录不能为空");
        }
        return Paths.get(driveInfo.getInstallPath()).toAbsolutePath().normalize();
    }

    private List<String> getMonitorContainerIds(DriveInfo driveInfo, Path driveDir) {
        try {
            List<String> containerIds = getContainerIds(driveDir);
            if (containerIds.isEmpty()) {
                throw new ServiceException("未查询到驱动容器");
            }
            return containerIds;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取驱动容器失败，driveId={}，installPath={}", driveInfo.getId(), driveInfo.getInstallPath(), e);
            throw new ServiceException("获取驱动容器失败");
        }
    }

    private List<ContainerStats> getContainerStats(List<String> containerIds) {
        List<ContainerStats> containerStats = new ArrayList<>();
        if (containerIds == null || containerIds.isEmpty()) {
            return containerStats;
        }

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("stats");
        command.add("--no-stream");
        command.add("--format");
        command.add("{{json .}}");
        command.addAll(containerIds);

        try {
            CommandResult result = executeCommand(null, command.toArray(new String[0]));
            if (result.exitCode != 0) {
                throw new ServiceException("查询容器资源失败：" + result.output);
            }
            for (String line : result.output.split("\\R")) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                JSONObject statsObj = JSONUtil.parseObj(line);
                containerStats.add(parseContainerStats(statsObj));
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询容器资源失败，containerIds={}", containerIds, e);
            throw new ServiceException("查询容器资源失败");
        }
        return containerStats;
    }

    private ContainerStats parseContainerStats(JSONObject statsObj) {
        ContainerStats stats = new ContainerStats();
        stats.setContainerId(statsObj.getStr("Container"));
        stats.setName(statsObj.getStr("Name"));
        stats.setCpuPercent(parsePercent(statsObj.getStr("CPUPerc")));
        stats.setMemoryPercent(parsePercent(statsObj.getStr("MemPerc")));

        long[] memory = parsePairBytes(statsObj.getStr("MemUsage"));
        stats.setMemUsage(memory[0]);
        stats.setMemLimit(memory[1]);

        long[] network = parsePairBytes(statsObj.getStr("NetIO"));
        stats.setNetworkRx(network[0]);
        stats.setNetworkTx(network[1]);

        long[] block = parsePairBytes(statsObj.getStr("BlockIO"));
        stats.setBlockRead(block[0]);
        stats.setBlockWrite(block[1]);
        stats.setTimestamp(System.currentTimeMillis());
        return stats;
    }

    private ContainerInspectInfo getContainerInspectInfo(List<String> containerIds) {
        ContainerInspectInfo inspectInfo = new ContainerInspectInfo();
        if (containerIds == null || containerIds.isEmpty()) {
            return inspectInfo;
        }

        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("inspect");
        command.add("--size");
        command.addAll(containerIds);

        try {
            CommandResult result = executeCommand(null, command.toArray(new String[0]));
            if (result.exitCode != 0) {
                throw new ServiceException("查询容器详情失败：" + result.output);
            }
            JSONArray inspectArray = JSONUtil.parseArray(result.output);
            for (Object inspectItem : inspectArray) {
                JSONObject inspectObj = JSONUtil.parseObj(inspectItem);
                inspectInfo.setWritableSize(inspectInfo.getWritableSize() + defaultLong(inspectObj.getLong("SizeRw")));
                inspectInfo.setRootFsSize(inspectInfo.getRootFsSize() + defaultLong(inspectObj.getLong("SizeRootFs")));
                if (StringUtils.isBlank(inspectInfo.getIpAddress())) {
                    fillNetworkInfo(inspectObj, inspectInfo);
                }
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询容器详情失败，containerIds={}", containerIds, e);
            throw new ServiceException("查询容器详情失败");
        }
        return inspectInfo;
    }

    private void fillNetworkInfo(JSONObject inspectObj, ContainerInspectInfo inspectInfo) {
        JSONObject networkSettings = inspectObj.getJSONObject("NetworkSettings");
        if (networkSettings == null) {
            return;
        }
        JSONObject networks = networkSettings.getJSONObject("Networks");
        if (networks == null || networks.isEmpty()) {
            inspectInfo.setIpAddress(networkSettings.getStr("IPAddress"));
            return;
        }
        for (String networkName : networks.keySet()) {
            JSONObject networkObj = JSONUtil.parseObj(networks.get(networkName));
            String ipAddress = networkObj.getStr("IPAddress");
            if (StringUtils.isNotBlank(ipAddress)) {
                inspectInfo.setNetworkName(networkName);
                inspectInfo.setIpAddress(ipAddress);
                return;
            }
        }
    }

    private String buildContainerStatus(List<String> containerIds) {
        if (containerIds == null || containerIds.isEmpty()) {
            return "未运行";
        }
        try {
            for (String containerId : containerIds) {
                if (!isContainerRunning(containerId)) {
                    return "异常";
                }
            }
            return "运行中";
        } catch (Exception e) {
            log.warn("获取容器运行状态失败，containerIds={}", containerIds, e);
            return "未知";
        }
    }

    private List<String> getContainerNames(List<ContainerStats> containerStats) {
        List<String> names = new ArrayList<>();
        if (containerStats == null) {
            return names;
        }
        for (ContainerStats stats : containerStats) {
            if (StringUtils.isNotBlank(stats.getName())) {
                names.add(stats.getName());
            }
        }
        return names;
    }

    private long calculateStatsInterval(List<ContainerStats> firstStats, List<ContainerStats> secondStats) {
        if (firstStats == null || firstStats.isEmpty() || secondStats == null || secondStats.isEmpty()) {
            return 1000L;
        }
        long firstTimestamp = firstStats.get(0).getTimestamp();
        long secondTimestamp = secondStats.get(0).getTimestamp();
        return Math.max(secondTimestamp - firstTimestamp, 1L);
    }

    private long sumNetworkRx(List<ContainerStats> containerStats) {
        long total = 0L;
        if (containerStats == null) {
            return total;
        }
        for (ContainerStats stats : containerStats) {
            total += stats.getNetworkRx();
        }
        return total;
    }

    private long sumNetworkTx(List<ContainerStats> containerStats) {
        long total = 0L;
        if (containerStats == null) {
            return total;
        }
        for (ContainerStats stats : containerStats) {
            total += stats.getNetworkTx();
        }
        return total;
    }

    private double parsePercent(String value) {
        if (StringUtils.isBlank(value)) {
            return 0D;
        }
        String number = value.replace("%", "").trim();
        if (!isDecimal(number)) {
            return 0D;
        }
        return Double.valueOf(number);
    }

    private long[] parsePairBytes(String value) {
        long[] result = new long[]{0L, 0L};
        if (StringUtils.isBlank(value)) {
            return result;
        }
        String[] parts = value.split("/");
        if (parts.length > 0) {
            result[0] = parseDockerSize(parts[0]);
        }
        if (parts.length > 1) {
            result[1] = parseDockerSize(parts[1]);
        }
        return result;
    }

    private long parseDockerSize(String value) {
        if (StringUtils.isBlank(value)) {
            return 0L;
        }
        String normalized = value.trim().replace(" ", "");
        String number = normalized.replaceAll("[^0-9.]", "");
        String unit = normalized.replaceAll("[0-9.]", "").toLowerCase(Locale.ROOT);
        if (!isDecimal(number)) {
            return 0L;
        }

        double size = Double.valueOf(number);
        if ("b".equals(unit) || StringUtils.isBlank(unit)) {
            return (long) size;
        }
        if ("kb".equals(unit)) {
            return (long) (size * 1000D);
        }
        if ("mb".equals(unit)) {
            return (long) (size * 1000D * 1000D);
        }
        if ("gb".equals(unit)) {
            return (long) (size * 1000D * 1000D * 1000D);
        }
        if ("tb".equals(unit)) {
            return (long) (size * 1000D * 1000D * 1000D * 1000D);
        }
        if ("kib".equals(unit)) {
            return (long) (size * 1024D);
        }
        if ("mib".equals(unit)) {
            return (long) (size * 1024D * 1024D);
        }
        if ("gib".equals(unit)) {
            return (long) (size * 1024D * 1024D * 1024D);
        }
        if ("tib".equals(unit)) {
            return (long) (size * 1024D * 1024D * 1024D * 1024D);
        }
        return (long) size;
    }

    private boolean isDecimal(String value) {
        return StringUtils.isNotBlank(value) && value.matches("\\d+(\\.\\d+)?");
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private long parseLong(String[] values, int index) {
        if (values.length <= index || !StringUtils.isNumeric(values[index])) {
            return 0L;
        }
        return Long.valueOf(values[index]);
    }

    private String buildResourceStatus(double usageRate) {
        return usageRate >= 80D ? "使用率偏高" : "正常";
    }

    private String buildStorageStatus(double usageRate) {
        return usageRate >= 80D ? "使用率偏高" : "正常";
    }

    private String formatPercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0%";
        }
        if (Math.abs(value - Math.round(value)) < 0.05D) {
            return String.format(Locale.US, "%.0f%%", value);
        }
        return String.format(Locale.US, "%.1f%%", value);
    }

    private String formatBytes(long bytes) {
        if (bytes <= 0L) {
            return "0 B";
        }
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB", "PB"};
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024D && unitIndex < units.length - 1) {
            value = value / 1024D;
            unitIndex++;
        }
        if (value >= 100D || Math.abs(value - Math.round(value)) < 0.05D) {
            return String.format(Locale.US, "%.0f %s", value, units[unitIndex]);
        }
        return String.format(Locale.US, "%.1f %s", value, units[unitIndex]);
    }

    private String formatBitRate(long bytes, long intervalMillis) {
        double bitsPerSecond = intervalMillis <= 0L ? 0D : bytes * 8D * 1000D / intervalMillis;
        String[] units = new String[]{"bps", "Kbps", "Mbps", "Gbps"};
        int unitIndex = 0;
        while (bitsPerSecond >= 1000D && unitIndex < units.length - 1) {
            bitsPerSecond = bitsPerSecond / 1000D;
            unitIndex++;
        }
        if (bitsPerSecond >= 100D || Math.abs(bitsPerSecond - Math.round(bitsPerSecond)) < 0.05D) {
            return String.format(Locale.US, "%.0f %s", bitsPerSecond, units[unitIndex]);
        }
        return String.format(Locale.US, "%.1f %s", bitsPerSecond, units[unitIndex]);
    }

    private void sleepQuietly(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendMsg(Long userId, String cmd, String message) {
        log.info("uploadDrive-sendMsg 用户【{}】发送指令【{}】", userId, cmd);
        CommonMsgVo commonMsgVo = new CommonMsgVo();
        commonMsgVo.setType(CommonWebsoketTypeEnum.DRIVE_UPLOAD);

        Map<String, String> data = new HashMap<>();
        data.put("message", message);
        data.put("cmd", cmd);

        commonMsgVo.setData(data);
        commonMsgWebsocket.sendMessage(userId, commonMsgVo);
    }

    private Long getUploadUserId(DriveDTO dto) {
        if (dto.getUserId() != null) {
            return dto.getUserId();
        }
        return SecurityUtils.getUserId();
    }

    private DriveUploadFile validateUploadFile(DriveDTO dto) {
        if (dto == null) {
            throw new ServiceException("请上传驱动文件");
        }

        DriveUploadFile uploadFile;
        if (StringUtils.isNotBlank(dto.getTempFilePath())) {
            uploadFile = DriveUploadFile.ofTempFile(dto.getOriginalFilename(), Paths.get(dto.getTempFilePath()));
        } else if (dto.getFile() != null && !dto.getFile().isEmpty()) {
            uploadFile = DriveUploadFile.ofMultipartFile(dto.getFile());
        } else {
            throw new ServiceException("请上传驱动文件");
        }

        String originalFilename = uploadFile.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename) || !"zip".equalsIgnoreCase(FilenameUtils.getExtension(originalFilename))) {
            throw new ServiceException("驱动只支持zip格式");
        }
        if (!uploadFile.exists()) {
            throw new ServiceException("驱动上传临时文件不存在，请重新上传");
        }
        return uploadFile;
    }

    private String sanitizeDriveName(String fileName) {
        String driveName = StringUtils.defaultString(fileName)
                .replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", StringUtils.EMPTY);
        if (StringUtils.isBlank(driveName)) {
            throw new ServiceException("驱动文件名称不合法");
        }
        return driveName;
    }

    private void createDriveDirectory(Path driveDir) throws IOException {
        Path installDir = Paths.get(installPath).toAbsolutePath().normalize();
        Files.createDirectories(installDir);
        checkDirectory(installDir, "驱动安装根目录");

        Files.createDirectories(driveDir);
        checkDirectory(driveDir, "驱动目录");
        log.info("uploadDrive 驱动目录创建完成，安装根目录：【{}】，根目录状态：【exists={}, directory={}, writable={}】，驱动目录：【{}】，驱动目录状态：【exists={}, directory={}, writable={}】",
                installDir, Files.exists(installDir), Files.isDirectory(installDir), Files.isWritable(installDir),
                driveDir, Files.exists(driveDir), Files.isDirectory(driveDir), Files.isWritable(driveDir));
    }

    private void checkDirectory(Path directory, String directoryName) {
        if (!Files.exists(directory)) {
            throw new ServiceException(directoryName + "创建失败，目录不存在：" + directory);
        }
        if (!Files.isDirectory(directory)) {
            throw new ServiceException(directoryName + "创建失败，路径不是目录：" + directory);
        }
        if (!Files.isWritable(directory)) {
            throw new ServiceException(directoryName + "不可写：" + directory);
        }
    }

    private void unzip(DriveUploadFile file, Path targetDir) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir)) {
                    throw new ServiceException("驱动压缩包包含非法路径：" + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Path parent = entryPath.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipInputStream.closeEntry();
            }
        }
    }

    private int findAvailablePort(int startPort) {
        Integer maxPort = deviceInfoMapper.selectMaxPort();
        if (maxPort != null && maxPort > 0) {
            startPort = maxPort + 1;
        }

        for (int port = startPort; port <= 65535; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new ServiceException("未找到可用端口");
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void replaceHostPort(Path composeFile, int hostPort) throws IOException {
        String content = new String(Files.readAllBytes(composeFile), StandardCharsets.UTF_8);
        if (!content.contains("HOST_MACHINE_PORT")) {
            throw new ServiceException("docker-compose.yml文件缺少HOST_MACHINE_PORT占位符");
        }
        Files.write(composeFile, content.replace("HOST_MACHINE_PORT", String.valueOf(hostPort)).getBytes(StandardCharsets.UTF_8));
    }

    private List<String> getContainerIds(Path workDir) throws IOException, InterruptedException {
        CommandResult result = executeDockerComposeCommand(workDir, "ps", "-q");
        if (result.exitCode != 0) {
            throw new ServiceException("查询docker容器失败：" + result.output);
        }

        List<String> containerIds = new ArrayList<>();
        for (String line : result.output.split("\\R")) {
            if (StringUtils.isNotBlank(line)) {
                containerIds.add(line.trim());
            }
        }
        return containerIds;
    }

    private boolean isContainerRunning(String containerId) throws IOException, InterruptedException {
        CommandResult result = executeCommand(null, "docker", "inspect", "-f", "{{.State.Running}}", containerId);
        return result.exitCode == 0 && "true".equalsIgnoreCase(result.output.trim());
    }

    private CommandResult executeDockerComposeCommand(Path workDir, String... args) throws IOException, InterruptedException {
        String[] dockerComposeCommand = buildCommand("docker-compose", args);
        try {
            return executeCommand(workDir, dockerComposeCommand);
        } catch (IOException e) {
            log.warn("uploadDrive 执行docker-compose失败，尝试使用docker compose命令，原因：{}", e.getMessage());
            return executeCommand(workDir, buildCommand("docker", appendFirst("compose", args)));
        }
    }

    private String[] appendFirst(String first, String[] args) {
        String[] command = new String[args.length + 1];
        command[0] = first;
        System.arraycopy(args, 0, command, 1, args.length);
        return command;
    }

    private String[] buildCommand(String command, String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = command;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }

    private CommandResult executeCommand(Path workDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (workDir != null) {
            checkDirectory(workDir, "命令执行目录");
            processBuilder.directory(workDir.toFile());
        }
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        boolean finished = process.waitFor(300, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new ServiceException("执行命令超时：" + StringUtils.join(command, " "));
        }

        String output = readProcessOutput(process);
        log.info("uploadDrive 执行命令：{}，退出码：{}，输出：{}", StringUtils.join(command, " "), process.exitValue(), output);
        return new CommandResult(process.exitValue(), output);
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        return output.toString().trim();
    }

    private static class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

    }

    private static class CpuSnapshot {
        private final long total;
        private final long idle;

        private CpuSnapshot(long total, long idle) {
            this.total = total;
            this.idle = idle;
        }
    }

    private static class NetworkSnapshot {
        private final long rxBytes;
        private final long txBytes;
        private final long timestamp;

        private NetworkSnapshot(long rxBytes, long txBytes, long timestamp) {
            this.rxBytes = rxBytes;
            this.txBytes = txBytes;
            this.timestamp = timestamp;
        }
    }

    @Data
    private static class ContainerStats {
        private String containerId;
        private String name;
        private double cpuPercent;
        private double memoryPercent;
        private long memUsage;
        private long memLimit;
        private long networkRx;
        private long networkTx;
        private long blockRead;
        private long blockWrite;
        private long timestamp;
    }

    @Data
    private static class ContainerInspectInfo {
        private long writableSize;
        private long rootFsSize;
        private String ipAddress;
        private String networkName;
    }

    private static class DriveUploadFile {
        private final String originalFilename;
        private final MultipartFile multipartFile;
        private final Path tempFile;

        private DriveUploadFile(String originalFilename, MultipartFile multipartFile, Path tempFile) {
            this.originalFilename = originalFilename;
            this.multipartFile = multipartFile;
            this.tempFile = tempFile;
        }

        private static DriveUploadFile ofMultipartFile(MultipartFile multipartFile) {
            return new DriveUploadFile(multipartFile.getOriginalFilename(), multipartFile, null);
        }

        private static DriveUploadFile ofTempFile(String originalFilename, Path tempFile) {
            return new DriveUploadFile(originalFilename, null, tempFile);
        }

        private String getOriginalFilename() {
            return originalFilename;
        }

        private InputStream getInputStream() throws IOException {
            if (tempFile != null) {
                return Files.newInputStream(tempFile);
            }
            return multipartFile.getInputStream();
        }

        private boolean exists() {
            return tempFile == null || Files.exists(tempFile);
        }

        private void deleteTempFile() {
            if (tempFile == null) {
                return;
            }
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("删除驱动上传临时文件失败：{}", tempFile, e);
            }
        }
    }
}
