package com.scse.curriculum.auditlog.config;

import com.scse.curriculum.auditlog.listener.AuditLogHibernateListener;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HibernateListenerConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final AuditLogHibernateListener auditLogHibernateListener;

    @PostConstruct
    public void registerListeners() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);

        if (registry != null) {
            registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(auditLogHibernateListener);
            registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(auditLogHibernateListener);
            registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(auditLogHibernateListener);
        }
    }
}
