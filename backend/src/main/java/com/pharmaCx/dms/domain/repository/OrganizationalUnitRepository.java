package com.pharmaCx.dms.domain.repository;

import com.pharmaCx.dms.domain.model.OrganizationalUnit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationalUnitRepository extends MongoRepository<OrganizationalUnit, String> {

    Optional<OrganizationalUnit> findByCode(String code);

    List<OrganizationalUnit> findByActiveTrue();

    List<OrganizationalUnit> findByParentUnitId(String parentUnitId);

    boolean existsByCode(String code);
}
