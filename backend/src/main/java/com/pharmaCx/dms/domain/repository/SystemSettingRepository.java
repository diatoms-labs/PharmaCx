package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.model.SystemSetting;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SystemSettingRepository extends MongoRepository<SystemSetting, String> {

    Optional<SystemSetting> findByScopeAndScopeId(String scope, String scopeId);

    // Convenience: find GLOBAL setting (scopeId is null)
    Optional<SystemSetting> findByScopeAndScopeIdIsNull(String scope);
}
