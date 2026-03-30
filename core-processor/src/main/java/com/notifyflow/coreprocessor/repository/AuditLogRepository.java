package com.notifyflow.coreprocessor.repository;

import com.notifyflow.coreprocessor.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}