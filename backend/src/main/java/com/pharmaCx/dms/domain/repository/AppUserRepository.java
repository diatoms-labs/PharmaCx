package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.enums.UserRole;
import com.pharmaCx.dms.domain.model.AppUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends MongoRepository<AppUser, String> {

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<AppUser> findByActiveTrue();

    List<AppUser> findByUnitIdAndActiveTrue(String unitId);

    List<AppUser> findByRoleAndActiveTrue(UserRole role);

    List<AppUser> findByUnitIdAndRoleAndActiveTrue(String unitId, UserRole role);
}
