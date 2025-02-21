package org.folio.roles.repository;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.entity.CapabilityEndpointEntity;
import org.folio.roles.domain.entity.CapabilityEndpointEntity.CapabilityEndpointPrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CapabilityEndpointRepository
  extends JpaRepository<CapabilityEndpointEntity, CapabilityEndpointPrimaryKey> {

  @Query(nativeQuery = true, value = """
    SELECT DISTINCT ce.* FROM capability_endpoint ce
      JOIN role_capability rc ON ce.capability_id = rc.capability_id AND rc.role_id = :roleId
      JOIN capability c ON c.id = ce.capability_id AND c.dummy_capability = false
    WHERE (:capabilityIds IS NULL OR ce.capability_id NOT IN (
      SELECT * FROM UNNEST(CAST(STRING_TO_ARRAY(:capabilityIds, ',') AS UUID[]))))""")
  List<CapabilityEndpointEntity> getByRoleId(
    @Param("roleId") UUID roleId,
    @Param("capabilityIds") String capabilityIds);

  @Query(nativeQuery = true, value = """
    SELECT DISTINCT ce.* FROM capability_endpoint ce
      JOIN capability_set_capability csc ON csc.capability_id = ce.capability_id
      JOIN capability c ON c.id = ce.capability_id AND c.dummy_capability = false
      JOIN role_capability_set rcs ON csc.capability_set_id = rcs.capability_set_id AND rcs.role_id = :roleId
    WHERE (:capabilityIds IS NULL
        OR ce.capability_id NOT IN (SELECT * FROM UNNEST(CAST(STRING_TO_ARRAY(:capabilityIds, ',') AS UUID[]))))
      AND (:capabilitySetIds IS NULL
        OR csc.capability_id NOT IN (SELECT * FROM UNNEST(CAST(STRING_TO_ARRAY(:capabilitySetIds, ',') AS UUID[]))))""")
  List<CapabilityEndpointEntity> getByRoleId(
    @Param("roleId") UUID roleId,
    @Param("capabilityIds") String capabilityIds,
    @Param("capabilitySetIds") String capabilitySetIds);

  @Query(nativeQuery = true, value = """
    SELECT DISTINCT ce.* FROM capability_endpoint ce
      JOIN user_capability uc ON ce.capability_id = uc.capability_id AND uc.user_id = :userId
      JOIN capability c ON c.id = ce.capability_id AND c.dummy_capability = false
    WHERE (:capabilityIds IS NULL OR ce.capability_id NOT IN (
      SELECT * FROM UNNEST(CAST(STRING_TO_ARRAY(:capabilityIds, ',') AS UUID[]))))""")
  List<CapabilityEndpointEntity> getByUserId(
    @Param("userId") UUID userId, @Param("capabilityIds") String capabilityIds);

  @Query(nativeQuery = true, value = """
    SELECT DISTINCT ce.* FROM capability_endpoint ce
      JOIN capability_set_capability csc ON csc.capability_id = ce.capability_id
      JOIN capability c ON c.id = ce.capability_id AND c.dummy_capability = false
      JOIN user_capability_set ucs ON csc.capability_set_id = ucs.capability_set_id AND ucs.user_id = :userId
    WHERE (:capabilityIds IS NULL
        OR ce.capability_id NOT IN (SELECT * FROM UNNEST(CAST(STRING_TO_ARRAY(:capabilityIds, ',') AS UUID[]))))
      AND (:capabilitySetIds IS NULL
        OR csc.capability_id NOT IN (SELECT * FROM UNNEST(CAST(STRING_TO_ARRAY(:capabilitySetIds, ',') AS UUID[]))))""")
  List<CapabilityEndpointEntity> getByUserId(
    @Param("userId") UUID userId,
    @Param("capabilityIds") String capabilityIds,
    @Param("capabilitySetIds") String capabilitySetIds);
}
