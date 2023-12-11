package org.folio.roles.repository;

import java.util.Optional;
import java.util.UUID;
import org.folio.roles.domain.entity.BasePolicyEntity;
import org.folio.spring.cql.JpaCqlRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyEntityRepository extends JpaCqlRepository<BasePolicyEntity, UUID> {

  Optional<BasePolicyEntity> findByName(String name);
}
