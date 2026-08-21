package com.scse.curriculum.auditlog.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scse.curriculum.auditlog.entity.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogHibernateListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public void onPostInsert(PostInsertEvent event) {
        if (isAuditable(event.getEntity())) {
            publishEvent(event.getEntity(), event.getId(), "CREATE", null, event.getState(), event.getPersister());
        }
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        if (isAuditable(event.getEntity())) {
            publishEvent(event.getEntity(), event.getId(), "UPDATE", event.getOldState(), event.getState(), event.getPersister());
        }
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        if (isAuditable(event.getEntity())) {
            publishEvent(event.getEntity(), event.getId(), "DELETE", event.getDeletedState(), null, event.getPersister());
        }
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    private boolean isAuditable(Object entity) {
        // We do not want to audit the AuditLog itself!
        return !(entity instanceof AuditLog);
    }

    private void publishEvent(Object entity, Object entityId, String action, Object[] oldState, Object[] newState, EntityPersister persister) {
        try {
            String tableName = getTableName(entity);
            Integer recordId = extractRecordId(entityId);
            
            if (recordId == null) {
                log.warn("Could not extract Integer ID for entity: {}", tableName);
                return;
            }

            String oldJson = toJson(oldState, persister.getPropertyNames());
            String newJson = toJson(newState, persister.getPropertyNames());

            // Get Current User Info.
            // FR-01.7 fix: chỉ lấy username từ SecurityContext (không query DB ở đây,
            // vì listener đang chạy giữa lúc Hibernate flush -> query DB ngay bây giờ
            // có thể gây reentrant flush). Việc phân giải username -> user_account.id
            // được AuditLogAsyncProcessor thực hiện an toàn trên thread @Async riêng.
            String username = getCurrentUsername();
            String ipAddress = getIpAddress();
            String userAgent = getUserAgent();

            AuditLogEvent auditEvent = new AuditLogEvent(
                    this,
                    tableName,
                    recordId,
                    action,
                    oldJson,
                    newJson,
                    username,
                    LocalDateTime.now(),
                    ipAddress,
                    userAgent
            );
            
            applicationEventPublisher.publishEvent(auditEvent);

        } catch (Exception e) {
            log.error("Failed to process audit event for entity: {}", entity.getClass().getSimpleName(), e);
        }
    }

    private String getTableName(Object entity) {
        jakarta.persistence.Table tableAnnotation = entity.getClass().getAnnotation(jakarta.persistence.Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }
        return entity.getClass().getSimpleName().toLowerCase();
    }

    private Integer extractRecordId(Object id) {
        if (id instanceof Integer) {
            return (Integer) id;
        } else if (id instanceof Long) {
            return ((Long) id).intValue();
        }
        return null;
    }

    private String toJson(Object[] state, String[] propertyNames) {
        if (state == null) return null;
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < propertyNames.length; i++) {
            Object value = state[i];
            map.put(propertyNames[i], simplifyValue(value));
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize state to JSON", e);
            return null;
        }
    }

    private Object simplifyValue(Object value) {
        if (value == null) return null;
        
        // Return primitives, wrappers, strings, enums directly
        if (value instanceof Number || value instanceof String || value instanceof Boolean || value instanceof Enum) {
            return value;
        }
        
        // For associated entities (ManyToOne / OneToOne), just extract ID to prevent infinite recursion
        try {
            java.lang.reflect.Method getIdMethod = value.getClass().getMethod("getId");
            return getIdMethod.invoke(value);
        } catch (Exception e) {
            // If it doesn't have a simple getId method or it's a collection, ignore or return class name
            // For audit logs, ignoring complex collections is usually preferred.
            return null; // Ignore collections/complex objects
        }
    }

    /**
     * FR-01.7 fix: nguyên bản kiểm tra {@code auth.getPrincipal() instanceof UserAccount},
     * nhưng principal thực tế được {@code JwtAuthFilter}/{@code CustomUserDetailsService}
     * gán là {@code org.springframework.security.core.userdetails.User} — KHÔNG BAO GIỜ
     * là {@code UserAccount}. Do đó điều kiện luôn false -> actor luôn null -> mọi thao
     * tác đều bị ghi nhận sai thành "Admin" (id=1) ở AuditLogAsyncProcessor.
     *
     * Sửa: chỉ cần lấy username (authentication.getName()), việc tra ra user_account.id
     * tương ứng sẽ do AuditLogAsyncProcessor thực hiện (an toàn để query DB ở đó).
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    private String getIpAddress() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader == null) {
                return request.getRemoteAddr();
            }
            return xfHeader.split(",")[0];
        }
        return "System";
    }

    private String getUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            return attrs.getRequest().getHeader("User-Agent");
        }
        return "System";
    }
}
