package org.folio.roles.service.permission;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.common.utils.permission.model.PermissionAction;
import org.folio.common.utils.permission.model.PermissionData;
import org.folio.common.utils.permission.model.PermissionType;
import org.folio.roles.configuration.property.PermissionMappingProperties;
import org.folio.roles.exception.ServiceException;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class PermissionOverrider {

  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;
  private final PermissionMappingProperties permissionMappingProperties;

  public Map<String, PermissionData> getPermissionMappings() {
    var sourcePath = permissionMappingProperties.getSourcePath();
    try {
      var resource = resourceLoader.getResource(sourcePath);
      if (resource.exists()) {
        var permissions =
          objectMapper.readValue(resource.getInputStream(), new TypeReference<Map<String, Permission>>() {});
        return permissions.entrySet().stream()
          .collect(toMap(Map.Entry::getKey, PermissionOverrider::mapToPermissionData));
      } else {
        log.warn("Source not found: {}", sourcePath);
        throw new NotFoundException("Source not found: " + sourcePath);
      }
    } catch (IOException e) {
      log.warn("Failed to load resource: {}", sourcePath, e);
      throw new ServiceException("Failed to load resource: " + sourcePath, e);
    }
  }

  private static PermissionData mapToPermissionData(Entry<String, Permission> key) {
    var permission = key.getValue();
    return PermissionData.builder()
      .action(PermissionAction.fromValue(permission.action()))
      .type(PermissionType.fromValue(permission.type()))
      .resource(permission.resource())
      .build();
  }

  public record Permission(String action, String resource, String type) {}
}
