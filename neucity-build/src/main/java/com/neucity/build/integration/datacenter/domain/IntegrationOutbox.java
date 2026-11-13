package com.neucity.build.integration.datacenter.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/** 来源侧事务发件箱。 */
@Data
@TableName("integration_outbox")
public class IntegrationOutbox implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String messageId;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String topicKey;
    private String payloadJson;
    private String status;
    private Integer attemptCount;
    private Date nextAttemptAt;
    private String claimToken;
    private Date claimUntil;
    private Date publishedAt;
    private String lastError;
    private Date createTime;
    private Date updateTime;
}
