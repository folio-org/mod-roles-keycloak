package org.folio.roles.mapper;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.Collection;
import java.util.List;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.integration.keyclock.model.KeycloakRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR)
public interface KeycloakRolesUserMapper {

  @Mapping(target = "composite", ignore = true)
  @Mapping(target = "clientRole", ignore = true)
  @Mapping(target = "containerId", ignore = true)
  KeycloakRole toKeycloakDto(Role userRole);

  List<KeycloakRole> toKeycloakDto(Collection<Role> userRole);
}
