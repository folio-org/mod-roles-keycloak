package org.folio.roles.mapper;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import org.folio.roles.domain.dto.Role;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR)
public interface KeycloakRoleMapper {

  @Mapping(target = "composite", ignore = true)
  @Mapping(target = "composites", ignore = true)
  @Mapping(target = "clientRole", ignore = true)
  @Mapping(target = "containerId", ignore = true)
  @Mapping(target = "attributes", ignore = true)
  RoleRepresentation toKeycloakRole(Role source);

  @Mapping(target = "metadata", ignore = true)
  Role toRole(RoleRepresentation source);
}
