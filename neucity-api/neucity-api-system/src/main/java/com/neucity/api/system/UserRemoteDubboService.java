package com.neucity.api.system;
import com.neucity.api.system.vo.UserVo;
import java.util.List;

/** 建筑设备状态通知所需的接收用户查询。 */
public interface UserRemoteDubboService {
    List<UserVo> selectUserByDeviceType(List<String> deviceTypes);
}
