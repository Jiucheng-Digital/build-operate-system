package com.neucity.build.integration.datacenter.mapper;

import com.neucity.build.integration.datacenter.domain.IntegrationOutbox;
import com.neucity.common.mybatisplus.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface DataCenterOutboxMapper extends BaseMapperPlus<IntegrationOutbox, IntegrationOutbox> {

    List<IntegrationOutbox> selectDue(@Param("now") Date now, @Param("limit") int limit);

    int claim(@Param("id") Long id, @Param("token") String token, @Param("now") Date now,
              @Param("claimUntil") Date claimUntil);

    int markPublished(@Param("id") Long id, @Param("token") String token,
                      @Param("publishedAt") Date publishedAt);

    int markRetry(@Param("id") Long id, @Param("token") String token,
                  @Param("nextAttemptAt") Date nextAttemptAt, @Param("lastError") String lastError,
                  @Param("updateTime") Date updateTime);

    int deletePublishedBefore(@Param("before") Date before, @Param("limit") int limit);
}
