package com.neucity.build.websocket;

import cn.hutool.json.JSONUtil;
import com.neucity.common.core.enums.CommonWebsoketTypeEnum;
import com.neucity.common.core.model.dto.websocket.CommonMsgDTO;
import com.neucity.common.core.model.dto.websocket.CommonMsgVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 建筑服务消息推送，连接地址需加上服务 context-path：/api/build。
 */
@Slf4j
@Component
@ServerEndpoint("/common/message/websocket/{userId}")
public class CommonMsgWebsocket {

    // Spring Bean 与 WebSocket 容器创建的端点实例共享会话。
    private static final Map<Long, Session> USER_SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        session.setMaxIdleTimeout(TimeUnit.SECONDS.toMillis(60));
        Session previousSession = USER_SESSION_MAP.put(userId, session);
        if (previousSession != null && previousSession != session) {
            closeSession(previousSession, userId, "Connection replaced");
        }
        log.info("Build CommonMsgWebsocket 用户【{}】连接成功", userId);
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") Long userId) {
        // 旧连接的关闭回调不能删除用户重连后建立的新会话。
        USER_SESSION_MAP.remove(userId, session);
        log.info("Build CommonMsgWebsocket 用户【{}】连接关闭", userId);
    }

    @OnError
    public void onError(Session session, Throwable error, @PathParam("userId") Long userId) {
        log.error("Build CommonMsgWebsocket 用户【{}】连接异常", userId, error);
        if (session != null) {
            USER_SESSION_MAP.remove(userId, session);
            closeSession(session, userId, "WebSocket error");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") Long userId) {
        try {
            CommonMsgDTO dto = JSONUtil.toBean(message, CommonMsgDTO.class);
            if (dto != null && CommonWebsoketTypeEnum.PING.equals(dto.getType())) {
                CommonMsgVo pong = new CommonMsgVo();
                pong.setType(CommonWebsoketTypeEnum.PING);
                pong.setData("pong");
                sendText(userId, session, pong);
            }
        } catch (RuntimeException e) {
            log.warn("Build CommonMsgWebsocket 用户【{}】消息解析失败", userId, e);
        }
    }

    public boolean sendMessage(Long userId, CommonMsgVo message) {
        if (userId == null || message == null) {
            log.warn("Build CommonMsgWebsocket 发送失败，用户ID或消息为空，userId: {}", userId);
            return false;
        }
        return sendText(userId, USER_SESSION_MAP.get(userId), message);
    }

    private boolean sendText(Long userId, Session session, CommonMsgVo message) {
        if (session == null) {
            log.warn("Build CommonMsgWebsocket 用户【{}】发送失败，连接不存在", userId);
            return false;
        }

        // 心跳和业务推送可能并发，按会话串行发送。
        synchronized (session) {
            if (!session.isOpen()) {
                USER_SESSION_MAP.remove(userId, session);
                return false;
            }
            try {
                session.getBasicRemote().sendText(JSONUtil.toJsonStr(message));
                return true;
            } catch (IOException | IllegalStateException e) {
                log.warn("Build CommonMsgWebsocket 用户【{}】发送消息失败", userId, e);
                return false;
            }
        }
    }

    private void closeSession(Session session, Long userId, String reason) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, reason));
        } catch (IOException | IllegalStateException e) {
            log.warn("Build CommonMsgWebsocket 用户【{}】关闭连接失败", userId, e);
        }
    }
}
