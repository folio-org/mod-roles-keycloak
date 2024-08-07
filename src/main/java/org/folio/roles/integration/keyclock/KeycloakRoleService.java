package org.folio.roles.integration.keyclock;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import feign.FeignException;
import jakarta.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.integration.keyclock.client.RoleClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.RoleMapper;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakRoleService {

  private final RoleClient roleClient;
  private final RoleMapper roleMapper;
  private final KeycloakAccessTokenService tokenService;
  private final FolioExecutionContext context;

  public Optional<Role> findById(UUID id) {
    try {
      var keycloakRole = roleClient.findById(context.getTenantId(), tokenService.getToken(), id);
      log.debug("Role has been found by id: id = {}, name = {}", id, keycloakRole.getName());

      return Optional.of(roleMapper.toRole(keycloakRole));
    } catch (FeignException.NotFound nf) {
      log.debug("Role hasn't been found by id: id = {}", id);

      return Optional.empty();
    } catch (FeignException fe) {
      throw new KeycloakApiException("Failed to find role: id = " + id, fe, fe.status());
    }
  }

  public Optional<Role> findByName(String name) {
    try {
      var keycloakRole = roleClient.findByName(context.getTenantId(), tokenService.getToken(), name);
      log.debug("Role has been found by name: name = {}", name);

      return Optional.of(roleMapper.toRole(keycloakRole));
    } catch (FeignException.NotFound e) {
      log.debug("Role hasn't been found by name: name = {}", name);
      return Optional.empty();
    } catch (FeignException e) {
      throw new KeycloakApiException("Failed to find role by name: name = " + name, e, e.status());
    }
  }

  public Role getById(UUID id) {
    return findById(id).orElseThrow(() -> new KeycloakApiException("Failed to find role: id = " + id,
        new NotFoundException("Could not find role with id"), NOT_FOUND.value()));
  }

  public Roles search(String query, Integer offset, Integer limit) {
    try {
      var keycloakRoles = roleClient.find(context.getTenantId(), tokenService.getToken(), offset, limit, query);
      var roles = keycloakRoles.stream().map(roleMapper::toRole).toList();
      log.debug("Roles have been found: names = {}", () -> extractRolesNames(roles));
      return buildRoles(roles);
    } catch (FeignException e) {
      throw new KeycloakApiException("Failed to search roles", e, e.status());
    }
  }

  public Role update(Role role) {
    var keycloakRole = roleMapper.toKeycloakRole(role);
    try {
      var accessToken = tokenService.getToken();
      roleClient.updateById(context.getTenantId(), accessToken, role.getId(), keycloakRole);
    } catch (FeignException e) {
      throw new KeycloakApiException("Failed to update role", e, e.status());
    }
    log.debug("Role has been updated: name = {}", role.getName());
    return role;
  }

  public void deleteById(UUID id) {
    try {
      roleClient.deleteById(context.getTenantId(), tokenService.getToken(), id);
    } catch (FeignException e) {
      throw new KeycloakApiException("Failed to delete role", e, e.status());
    }
    log.debug("Role has been deleted: id = {}", id);
  }

  public void deleteByIdSafe(UUID id) {
    try {
      roleClient.deleteById(context.getTenantId(), tokenService.getToken(), id);
      log.debug("Role has been deleted: id = {}", id);
    } catch (Exception e) {
      log.debug("Failed to delete Role in Keycloak: id = {}", id);
    }
  }

  public Role create(Role role) {
    try {
      var keycloakRole = roleMapper.toKeycloakRole(role);
      var accessToken = tokenService.getToken();
      roleClient.create(context.getTenantId(), accessToken, keycloakRole);

      var foundRole = roleClient.findByName(context.getTenantId(), accessToken, keycloakRole.getName());
      role.setId(foundRole.getId());

      log.debug("Role has been created: id = {}, name = {}", role.getId(), role.getName());
      return role;
    } catch (FeignException exception) {
      throw new KeycloakApiException("Failed to create keycloak role", exception, exception.status());
    }
  }

  public Optional<Role> createSafe(Role role) {
    try {
      return Optional.of(create(role));
    } catch (Exception e) {
      log.debug("Failed to create role: name = {}", role.getName(), e);
      return empty();
    }
  }

  private List<String> extractRolesNames(List<Role> createdKeycloakRoles) {
    return createdKeycloakRoles.stream().map(Role::getName).collect(toList());
  }

  private Roles buildRoles(List<Role> roles) {
    return new Roles().roles(roles).totalRecords(roles.size());
  }
}
