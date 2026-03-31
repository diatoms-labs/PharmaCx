package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.enums.AuditAction;
import com.pharmaCx.dms.domain.model.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {

    Page<AuditEvent> findByOrderByTimestampDesc(Pageable pageable);

    List<AuditEvent> findByResourceIdOrderByTimestampDesc(String resourceId);

    List<AuditEvent> findByUserIdOrderByTimestampDesc(String userId);

    Page<AuditEvent> findByActionOrderByTimestampDesc(AuditAction action, Pageable pageable);

    List<AuditEvent> findByTimestampBetweenOrderByTimestampDesc(Instant from, Instant to);

    List<AuditEvent> findTop20ByOrderByTimestampDesc();

    List<AuditEvent> findTop20ByUserIdOrderByTimestampDesc(String userId);
}
