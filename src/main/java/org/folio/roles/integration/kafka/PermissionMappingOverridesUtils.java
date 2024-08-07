package org.folio.roles.integration.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PermissionMappingOverridesUtils {

  protected static final Map<String, PermissionData> PERMISSION_MAPPING_OVERRIDES =
    getFolioPermissionMappingsOverrides();

  private static Map<String, PermissionData> getFolioPermissionMappingsOverrides() {
    var mappingOverridesFileLocation = "folio-permissions/mappings-overrides.json";
    try {
      var inStream =
        PermissionMappingOverridesUtils.class.getClassLoader().getResourceAsStream(mappingOverridesFileLocation);
      return new ObjectMapper().readValue(inStream, new TypeReference<>() {});
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse mapping overrides file: " + mappingOverridesFileLocation, e);
    }
  }

  public record PermissionData(String resource, String type, String action, String permission) {}
}
