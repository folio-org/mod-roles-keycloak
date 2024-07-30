package org.folio.roles.mapper;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import org.folio.roles.domain.dto.Role;
import org.folio.roles.integration.keyclock.model.KeycloakRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR)
public interface RoleMapper {

  @Mapping(target = "source", ignore = true)
  @Mapping(target = "composite", ignore = true)
  @Mapping(target = "clientRole", ignore = true)
  @Mapping(target = "containerId", ignore = true)
  KeycloakRole toKeycloakRole(Role source);

  @Mapping(target = "metadata", ignore = true)
  Role toRole(KeycloakRole source);
}
