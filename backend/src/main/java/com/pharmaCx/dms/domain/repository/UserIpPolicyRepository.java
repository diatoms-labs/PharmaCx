package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.model.UserIpPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserIpPolicyRepository extends MongoRepository<UserIpPolicy, String> {

    Optional<UserIpPolicy> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
